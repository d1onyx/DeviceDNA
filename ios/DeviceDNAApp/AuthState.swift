import Foundation
import UIKit
import FirebaseAuth
import GoogleSignIn
import FirebaseCore

struct AuthUser {
    let displayName: String
    let email: String
    let photoURL: URL?
}

@MainActor
class AuthState: ObservableObject {
    @Published var user: AuthUser?
    @Published var isConfigured = false
    @Published var isLoading = false
    @Published var errorMessage: String?

    var isSignedIn: Bool { user != nil }

    private var authStateHandle: AuthStateDidChangeListenerHandle?

    init() {
        if FirebaseApp.app() == nil,
           Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }

        isConfigured = FirebaseApp.app() != nil
        guard isConfigured else {
            return
        }

        authStateHandle = Auth.auth().addStateDidChangeListener { [weak self] _, firebaseUser in
            self?.user = firebaseUser.map {
                AuthUser(
                    displayName: $0.displayName ?? "DeviceDNA User",
                    email: $0.email ?? "",
                    photoURL: $0.photoURL
                )
            }
        }
    }

    deinit {
        if let handle = authStateHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    func signInWithGoogle() {
        guard isConfigured, let clientID = FirebaseApp.app()?.options.clientID else {
            showConfigurationError()
            return
        }
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else {
            errorMessage = "Cannot find root view controller."
            return
        }

        isLoading = true
        errorMessage = nil

        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config

        GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { [weak self] result, error in
            guard let self else { return }
            Task { @MainActor in
                self.isLoading = false
                if let error {
                    self.errorMessage = error.localizedDescription
                    return
                }
                guard let idToken = result?.user.idToken?.tokenString,
                      let accessToken = result?.user.accessToken.tokenString else {
                    self.errorMessage = "Google did not return tokens."
                    return
                }
                let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)
                do {
                    try await Auth.auth().signIn(with: credential)
                } catch {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func signOut() {
        guard isConfigured else {
            user = nil
            return
        }

        do {
            try Auth.auth().signOut()
            GIDSignIn.sharedInstance.signOut()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func showConfigurationError() {
        errorMessage = "Add GoogleService-Info.plist from Firebase, enable Google provider, add the reversed client ID URL scheme, then rebuild."
    }
}
