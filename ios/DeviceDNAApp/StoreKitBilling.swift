import Foundation
import StoreKit
import UIKit
import shared

/// StoreKit 2 billing bridged into the shared Kotlin `IosBillingGateway`.
///
/// App Store review (guideline 3.1.1): Premium is sold EXCLUSIVELY through Apple IAP —
/// an auto-renewable subscription configured in App Store Connect with the id below.
/// StoreKit's cryptographic verification is the entitlement source of truth; the Kotlin
/// side only caches the result.
final class StoreKitBilling {

    static let shared = StoreKitBilling()

    /// Auto-renewable subscription product id (must exist in App Store Connect).
    static let productId = AppConfig.storeKitPremiumProductId

    private(set) lazy var gateway = IosBillingGateway(
        purchaseAction: { done in
            Task { done(await StoreKitBilling.shared.purchase()) }
        },
        restoreAction: { done in
            Task {
                await StoreKitBilling.shared.syncWithAppStore()
                done(await StoreKitBilling.shared.currentOutcome(
                    missingMessage: String(localized: "No active subscription found.")))
            }
        },
        productInfoAction: { done in
            Task { done(await StoreKitBilling.shared.productInfo()) }
        },
        manageAction: {
            Task { await StoreKitBilling.shared.showSubscriptionManagement() }
        }
    )

    private var updatesTask: Task<Void, Never>?

    /// Call once at launch: re-derives entitlements from StoreKit (source of truth) and
    /// keeps listening for renewals/revocations delivered while the app runs.
    func start() {
        Task { await pushCurrentEntitlements() }
        updatesTask = Task(priority: .background) {
            for await verification in Transaction.updates {
                guard case .verified(let transaction) = verification else { continue }
                await StoreKitBilling.shared.pushCurrentEntitlements()
                await transaction.finish()
            }
        }
    }

    // MARK: - Flows

    private func purchase() async -> StoreKitOutcome {
        do {
            guard let product = try await Product.products(for: [Self.productId]).first else {
                return Self.failure(String(localized: "Subscription product is not available."))
            }
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                guard case .verified(let transaction) = verification else {
                    return Self.failure(String(localized: "Purchase could not be verified."))
                }
                await pushCurrentEntitlements()
                await transaction.finish()
                return Self.outcome(from: transaction)
            case .userCancelled:
                return Self.failure(String(localized: "Purchase cancelled."))
            case .pending:
                return Self.failure(String(localized: "Purchase is pending approval."))
            @unknown default:
                return Self.failure(String(localized: "Unknown purchase state."))
            }
        } catch {
            return Self.failure(error.localizedDescription)
        }
    }

    private func syncWithAppStore() async {
        try? await AppStore.sync()
    }

    private func productInfo() async -> SubscriptionProductInfo? {
        guard let product = try? await Product.products(for: [Self.productId]).first else {
            return nil
        }
        guard let period = product.subscription?.subscriptionPeriod else { return nil }
        let unit: String
        switch period.unit {
        case .day: unit = "day"
        case .week: unit = "week"
        case .month: unit = "month"
        case .year: unit = "year"
        @unknown default: return nil
        }
        return SubscriptionProductInfo(
            displayName: product.displayName,
            displayPrice: product.displayPrice,
            periodUnit: unit,
            periodValue: Int32(period.value)
        )
    }

    @MainActor
    private func showSubscriptionManagement() async {
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive })
        else { return }
        try? await AppStore.showManageSubscriptions(in: scene)
    }

    private func currentOutcome(missingMessage: String) async -> StoreKitOutcome {
        if let transaction = await activeTransaction() {
            return Self.outcome(from: transaction)
        }
        return Self.failure(missingMessage)
    }

    /// Re-reads Transaction.currentEntitlements and pushes the result into the Kotlin
    /// entitlements store — the single place premium state enters shared code.
    func pushCurrentEntitlements() async {
        let store = KoinBridge.shared.entitlementsStore()
        if let transaction = await activeTransaction() {
            let outcome = Self.outcome(from: transaction)
            try? await store.save(entitlements: gateway.toEntitlements(outcome: outcome))
        } else {
            try? await store.clear()
        }
    }

    private func activeTransaction() async -> Transaction? {
        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result,
                  transaction.productID == Self.productId,
                  transaction.revocationDate == nil
            else { continue }
            if let expiry = transaction.expirationDate, expiry <= Date() { continue }
            return transaction
        }
        return nil
    }

    // MARK: - Mapping

    private static func outcome(from transaction: Transaction) -> StoreKitOutcome {
        StoreKitOutcome(
            active: true,
            productId: transaction.productID,
            transactionId: String(transaction.id),
            purchasedAtMillis: Int64(transaction.purchaseDate.timeIntervalSince1970 * 1000),
            expiresAtMillis: transaction.expirationDate.map {
                KotlinLong(value: Int64($0.timeIntervalSince1970 * 1000))
            },
            errorMessage: nil
        )
    }

    private static func failure(_ message: String) -> StoreKitOutcome {
        StoreKitOutcome(
            active: false,
            productId: "",
            transactionId: "",
            purchasedAtMillis: 0,
            expiresAtMillis: nil,
            errorMessage: message
        )
    }
}
