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
        deleteAccountAction: { onResult in
            guard let user = Auth.auth().currentUser else { onResult("deleted"); return }
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
    }

    /// Launches the Google Sign-In sheet and exchanges the Google credential for Firebase.
    func signIn(forceAccountPicker: Bool) {
        DispatchQueue.main.async {
            // GIDSignIn.signIn(withPresenting:) raises an uncatchable NSException (not a
            // Swift error) if `.configuration` was never set — e.g. because Firebase
            // failed to configure. Guard on it explicitly so a misconfigured build fails
            // silently instead of crashing the whole app.
            guard GIDSignIn.sharedInstance.configuration != nil else { return }
            guard let presenter = Self.topViewController() else { return }
            if forceAccountPicker {
                GIDSignIn.sharedInstance.signOut()
            }
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                guard error == nil,
                      let idToken = result?.user.idToken?.tokenString,
                      let accessToken = result?.user.accessToken.tokenString
                else { return }
                let credential = GoogleAuthProvider.credential(
                    withIDToken: idToken,
                    accessToken: accessToken
                )
                Auth.auth().signIn(with: credential) { _, _ in
                    // State listener publishes the result into the Kotlin gateway.
                }
            }
        }
    }

    /// Launches the native "Sign in with Apple" flow and exchanges the Apple identity
    /// token for a Firebase credential. Requires the "Sign in with Apple" capability and
    /// Apple enabled as a provider in Firebase Auth (see ios/README.md).
    func signInWithApple() {
        DispatchQueue.main.async {
            let nonce = Self.randomNonceString()
            self.currentAppleNonce = nonce
            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            request.nonce = Self.sha256(nonce)

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests()
        }
    }

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap(\.windows).first { $0.isKeyWindow }
        var top = window?.rootViewController
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
        else { return }

        let credential = OAuthProvider.appleCredential(
            withIDToken: idToken,
            rawNonce: nonce,
            fullName: appleIDCredential.fullName
        )
        Auth.auth().signIn(with: credential) { _, _ in
            // State listener publishes the result into the Kotlin gateway.
        }
        currentAppleNonce = nil
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        // User cancelled or the request failed; leave auth state untouched.
        currentAppleNonce = nil
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.flatMap(\.windows).first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }
}
