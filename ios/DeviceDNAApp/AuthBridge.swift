import Foundation
import UIKit
import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import shared

/// Bridges Firebase Auth + Google Sign-In into the shared Kotlin `IosAuthGateway`.
/// Kotlin owns the auth *state* (Flow consumed by the Compose shell); Swift owns the
/// platform sign-in UI flow and token plumbing.
final class AuthBridge {

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
        }
    )

    private var listenerHandle: AuthStateDidChangeListenerHandle?

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

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap(\.windows).first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
