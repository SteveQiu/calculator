package com.example.calculator.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.example.calculator.model.ThemeId

/**
 * Wraps Google Play Billing (BillingClient) for one-time premium theme purchases.
 *
 * SKU convention: one product ID per premium theme, e.g. "theme_midnight", "theme_ocean", "theme_sunset".
 * Product type: INAPP (one-time purchase, not subscription).
 *
 * Flow:
 *   1. BillingClient connects on app start (call [connect]).
 *   2. [launchPurchaseFlow] queries ProductDetails then launches BillingFlowParams.
 *   3. [PurchasesUpdatedListener] receives the result; on OK → [onPurchaseValidated] callback fires.
 *   4. Caller (ThemeViewModel) calls ThemeRepository.unlockTheme() on success.
 *
 * TODO: Implement connect() — BillingClient.newBuilder().setListener(purchasesUpdatedListener).build()
 * TODO: Implement launchPurchaseFlow(activity, themeId) — queryProductDetailsAsync then launchBillingFlow
 * TODO: Implement acknowledgePurchase(purchase) — required for INAPP to avoid automatic refund
 * TODO: Handle BillingResponseCode.ITEM_ALREADY_OWNED — treat as unlock (restore purchases path)
 * TODO: Implement queryAndRestorePurchases() — call on app start to restore existing purchases
 */
class BillingRepository(
    private val context: Context,
    private val themeRepository: ThemeRepository,
) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient

    /** Map premium ThemeId to its Play Store product ID. */
    private val themeToProductId = mapOf(
        ThemeId.MIDNIGHT to "theme_midnight",
        ThemeId.OCEAN    to "theme_ocean",
        ThemeId.SUNSET   to "theme_sunset",
    )

    fun connect() {
        // TODO: Build and start BillingClient connection
    }

    fun launchPurchaseFlow(activity: Activity, themeId: ThemeId) {
        // TODO: queryProductDetailsAsync for themeToProductId[themeId]
        // TODO: launchBillingFlow with resulting ProductDetails
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        // TODO: Check billingResult.responseCode == BillingClient.BillingResponseCode.OK
        // TODO: For each purchase, acknowledgePurchase() then themeRepository.unlockTheme()
    }
}
