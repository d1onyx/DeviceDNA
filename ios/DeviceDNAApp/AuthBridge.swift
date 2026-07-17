import Foundation
import UIKit
import AuthenticationServices
import CryptoKit
import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import shared

/// Bridges Firebase Auth + Google Sign-In into the shared Kotlin `IosAuthGateway`.
/// Kotlin owns the auth *state* (Flow consumed by the Compose shell); Swift owns the
/// platform sign-in UI flow and token plumbing.
/// Inherits NSObject: ASAuthorizationControllerDelegate and
/// ASAuthorizationControllerPresentationContextProviding are Objective-C protocols that
/// refine NSObjectProtocol, so a conforming Swift type must be an NSObject subclass.
final class AuthBridge: NSObject {

    static let shared = AuthBridge()

    /// Kotlin gateway registered in Koin. Closures run on whatever thread Firebase calls
    /// back on; the gateway marshals state through a StateFlow, which is thread-safe.
    private(set) lazy var gateway = IosAuthGateway(
        configured: FirebaseApp.app() != nil,
        uidProvider: { Auth.auth().currentUser?.uid },
        tokenFetcher: { done in
            guard let user = Auth.auth().currentUser else { done(nil); return }
            user.getIDToken { token, _ in done(token) }
        },
        signOutAction: { done in
            try? Auth.auth().signOut()
            GIDSignIn.sharedInstance.signOut()
            done()
        },
        clearSessionAction: { removeGoogleAccount, done in
            try? Auth.auth().signOut()
            if removeGoogleAccount.boolValue {
                GIDSignIn.sharedInstance.disconnect { _ in done() }
            } else {
                GIDSignIn.sharedInstance.signOut()
                done()
            }
        },
        prepareDeletionAction: { onResult in
            guard let user = Auth.auth().currentUser else { onResult("ready"); return }
            // Kotlin exports the nested `(String) -> Unit` as `(String) -> KotlinUnit`, so it
            // cannot be handed to a Swift `(String) -> Void` parameter without this adapter.
            AuthBridge.shared.prepareDeletion(user: user) { _ = onResult($0) }
        },
        deleteAccountAction: { onResult in
            guard let user = Auth.auth().currentUser else { onResult("deleted"); return }
            if user.providerData.contains(where: { $0.providerID == "apple.com" }) {
                guard let authorizationCode = AuthBridge.shared.pendingAppleDeletionAuthorizationCode else {
                    onResult("reauth")
                    return
                }
                AuthBridge.shared.isDeletingAppleAccount = true
                Task { @MainActor in
                    defer { AuthBridge.shared.isDeletingAppleAccount = false }
                    do {
                        // Apple requires token revocation before deleting an account created with
                        // Sign in with Apple. The code comes from a fresh authorization request.
                        try await Auth.auth().revokeToken(withAuthorizationCode: authorizationCode)
                        try await user.delete()
                        AuthBridge.shared.pendingAppleDeletionAuthorizationCode = nil
                        GIDSignIn.sharedInstance.signOut()
                        onResult("deleted")
                    } catch let error as NSError where error.code == AuthErrorCode.requiresRecentLogin.rawValue {
                        onResult("reauth")
                    } catch {
                        NSLog("DeviceDNA/Auth: Apple account deletion failed: %@", error.localizedDescription)
                        onResult("failed")
                    }
                }
                return
            }
            let deleteNow: () -> Void = {
                user.delete { error in
                    if let ns = error as NSError?, ns.code == AuthErrorCode.requiresRecentLogin.rawValue {
                        onResult("reauth")
                    } else if error != nil {
                        onResult("failed")
                    } else {
                        GIDSignIn.sharedInstance.signOut()
                        onResult("deleted")
                    }
                }
            }
            // Refresh the Google credential first so delete isn't rejected for a stale login.
            GIDSignIn.sharedInstance.restorePreviousSignIn { gUser, _ in
                if let gUser = gUser, let idToken = gUser.idToken?.tokenString {
                    let credential = GoogleAuthProvider.credential(
                        withIDToken: idToken,
                        accessToken: gUser.accessToken.tokenString
                    )
                    user.reauthenticate(with: credential) { _, _ in deleteNow() }
                } else {
                    deleteNow()
                }
            }
        }
    )

    private var listenerHandle: AuthStateDidChangeListenerHandle?

    /// Raw nonce for the in-flight Apple Sign-In request; Firebase requires the un-hashed
    /// value when exchanging the Apple identity token for a Firebase credential.
    private var currentAppleNonce: String?

    /// Keeps the in-flight controller alive: `performRequests()` does not retain it, and a
    /// deallocated controller drops the request without ever showing the sheet or calling back.
    private var appleAuthController: ASAuthorizationController?

    /// A fresh Apple authorization code is single-use and retained only between the explicit
    /// reauthentication step and the immediately following Firebase account deletion.
    private var pendingAppleDeletionAuthorizationCode: String?
    private var appleDeletionCompletion: ((String) -> Void)?
    private var credentialRevokedObserver: NSObjectProtocol?
    private var isDeletingAppleAccount = false

    private func prepareDeletion(user: FirebaseAuth.User, onResult: @escaping (String) -> Void) {
        if user.providerData.contains(where: { $0.providerID == "apple.com" }) {
            DispatchQueue.main.async {
                guard Self.keyWindow() != nil else {
                    onResult("failed")
                    return
                }
                self.pendingAppleDeletionAuthorizationCode = nil
                self.appleDeletionCompletion = onResult
                let nonce = Self.randomNonceString()
                self.currentAppleNonce = nonce
                let request = ASAuthorizationAppleIDProvider().createRequest()
                request.nonce = Self.sha256(nonce)
                let controller = ASAuthorizationController(authorizationRequests: [request])
                controller.delegate = self
                controller.presentationContextProvider = self
                self.appleAuthController = controller
                controller.performRequests()
            }
            return
        }

        if let lastSignIn = user.metadata.lastSignInDate,
           Date().timeIntervalSince(lastSignIn) <= 4 * 60 {
            onResult("ready")
            return
        }

        GIDSignIn.sharedInstance.restorePreviousSignIn { gUser, _ in
            guard let gUser, let idToken = gUser.idToken?.tokenString else {
                onResult("reauth")
                return
            }
            let credential = GoogleAuthProvider.credential(
                withIDToken: idToken,
                accessToken: gUser.accessToken.tokenString
            )
            user.reauthenticate(with: credential) { _, error in
                if let ns = error as NSError?, ns.code == AuthErrorCode.requiresRecentLogin.rawValue {
                    onResult("reauth")
                } else if error != nil {
                    onResult("failed")
                } else {
                    onResult("ready")
                }
            }
        }
    }

    /// Pushes every Firebase auth-state change into the Kotlin gateway. The first callback
    /// clears `isInitializing`, letting the Compose shell leave the loading gate.
    func startListening() {
        guard FirebaseApp.app() != nil else {
            gateway.updateUser(uid: nil, displayName: nil, email: nil, photoUrl: nil)
            return
        }
        listenerHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            self?.gateway.updateUser(
                uid: user?.uid,
                displayName: user?.displayName,
                email: user?.email,
                photoUrl: user?.photoURL?.absoluteString
            )
        }
        credentialRevokedObserver = NotificationCenter.default.addObserver(
            forName: ASAuthorizationAppleIDProvider.credentialRevokedNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            guard self?.isDeletingAppleAccount != true else { return }
            try? Auth.auth().signOut()
            self?.gateway.updateUser(uid: nil, displayName: nil, email: nil, photoUrl: nil)
        }
    }

    /// Every failure path also pushes the reason into the Kotlin gateway: the shared auth screen
    /// renders `AuthUiState.errorMessage`, and a silent `return` here is indistinguishable from a
    /// dead button on screen.
    private func fail(_ message: String) {
        NSLog("DeviceDNA/Auth: %@", message)
        gateway.signInFailed(message: message)
    }

    /// Launches the Google Sign-In sheet and exchanges the Google credential for Firebase.
    func signIn(forceAccountPicker: Bool) {
        DispatchQueue.main.async {
            NSLog("DeviceDNA/Auth: Google sign-in requested forceAccountPicker=%@", forceAccountPicker ? "true" : "false")
            self.gateway.signInStarted()
            // GIDSignIn.signIn(withPresenting:) raises an uncatchable NSException (not a
            // Swift error) if `.configuration` was never set — e.g. because Firebase
            // failed to configure. Guard on it explicitly so a misconfigured build fails
            // silently instead of crashing the whole app.
            guard GIDSignIn.sharedInstance.configuration != nil else {
                self.fail("Google Sign-In is not configured (missing GoogleService-Info.plist).")
                return
            }
            guard let presenter = Self.topViewController() else {
                self.fail("Google sign-in could not find a window to present from.")
                return
            }
            if forceAccountPicker {
                GIDSignIn.sharedInstance.signOut()
            }
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if let error {
                    if (error as NSError).code == GIDSignInError.canceled.rawValue {
                        self.gateway.signInCancelled()
                    } else {
                        self.fail("Google sign-in failed: \(error.localizedDescription)")
                    }
                    return
                }
                guard let idToken = result?.user.idToken?.tokenString,
                      let accessToken = result?.user.accessToken.tokenString
                else {
                    self.fail("Google sign-in did not return both ID and access tokens.")
                    return
                }
                let credential = GoogleAuthProvider.credential(
                    withIDToken: idToken,
                    accessToken: accessToken
                )
                Auth.auth().signIn(with: credential) { _, error in
                    if let error {
                        self.fail("Firebase rejected the Google credential: \(error.localizedDescription)")
                    }
                    // On success the state listener publishes the user into the Kotlin gateway,
                    // which clears isSigningIn.
                }
            }
        }
    }

    /// Launches the native "Sign in with Apple" flow and exchanges the Apple identity
    /// token for a Firebase credential. Requires the "Sign in with Apple" capability and
    /// Apple enabled as a provider in Firebase Auth (see ios/README.md).
    func signInWithApple() {
        DispatchQueue.main.async {
            NSLog("DeviceDNA/Auth: Apple sign-in requested")
            self.gateway.signInStarted()
            guard Self.keyWindow() != nil else {
                self.fail("Apple sign-in could not find a window to present from.")
                return
            }
            let nonce = Self.randomNonceString()
            self.currentAppleNonce = nonce
            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            request.nonce = Self.sha256(nonce)

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            self.appleAuthController = controller
            controller.performRequests()
        }
    }

    /// `isKeyWindow` is not guaranteed to be set on the window hosting the Compose UI (a SwiftUI
    /// `WindowGroup` scene can be foreground-active with no key window yet). Falling back to the
    /// active scene's first window keeps both provider flows presentable instead of failing.
    static func keyWindow() -> UIWindow? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        if let key = scenes.flatMap(\.windows).first(where: \.isKeyWindow) { return key }
        let active = scenes.first { $0.activationState == .foregroundActive } ?? scenes.first
        return active?.windows.first
    }

    private static func topViewController() -> UIViewController? {
        var top = keyWindow()?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }

    // MARK: Apple nonce helpers

    private static func randomNonceString(length: Int = 32) -> String {
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remaining = length
        while remaining > 0 {
            var random: UInt8 = 0
            let status = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
            if status != errSecSuccess { continue }
            if random < charset.count {
                result.append(charset[Int(random) % charset.count])
                remaining -= 1
            }
        }
        return result
    }

    private static func sha256(_ input: String) -> String {
        let hashed = SHA256.hash(data: Data(input.utf8))
        return hashed.map { String(format: "%02x", $0) }.joined()
    }
}

// MARK: - Apple Sign-In delegates

extension AuthBridge: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let nonce = currentAppleNonce,
              let tokenData = appleIDCredential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8)
        else {
            if let completion = appleDeletionCompletion {
                appleDeletionCompletion = nil
                completion("failed")
            } else {
                fail("Apple sign-in did not return a usable identity token.")
            }
            appleAuthController = nil
            return
        }

        let credential = OAuthProvider.appleCredential(
            withIDToken: idToken,
            rawNonce: nonce,
            fullName: appleIDCredential.fullName
        )
        if let deletionCompletion = appleDeletionCompletion {
            appleDeletionCompletion = nil
            guard let authorizationCodeData = appleIDCredential.authorizationCode,
                  let authorizationCode = String(data: authorizationCodeData, encoding: .utf8),
                  let user = Auth.auth().currentUser
            else {
                deletionCompletion("failed")
                currentAppleNonce = nil
                appleAuthController = nil
                return
            }
            user.reauthenticate(with: credential) { _, error in
                if let ns = error as NSError?, ns.code == AuthErrorCode.requiresRecentLogin.rawValue {
                    deletionCompletion("reauth")
                } else if let error {
                    NSLog("DeviceDNA/Auth: Apple reauthentication failed: %@", error.localizedDescription)
                    deletionCompletion("failed")
                } else {
                    self.pendingAppleDeletionAuthorizationCode = authorizationCode
                    deletionCompletion("ready")
                }
            }
        } else {
            Auth.auth().signIn(with: credential) { _, error in
                if let error {
                    self.fail("Firebase rejected the Apple credential: \(error.localizedDescription)")
                }
                // On success the state listener publishes the user into the Kotlin gateway.
            }
        }
        currentAppleNonce = nil
        appleAuthController = nil
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        if let deletionCompletion = appleDeletionCompletion {
            appleDeletionCompletion = nil
            deletionCompletion((error as? ASAuthorizationError)?.code == .canceled ? "reauth" : "failed")
        } else if (error as? ASAuthorizationError)?.code == .canceled {
            gateway.signInCancelled()
        } else {
            fail("Apple sign-in failed: \(error.localizedDescription)")
        }
        currentAppleNonce = nil
        appleAuthController = nil
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // Never hand back a detached `ASPresentationAnchor()`: the sheet would be presented into
        // a window that is not on screen, i.e. nothing visibly happens.
        Self.keyWindow() ?? ASPresentationAnchor()
    }
}
