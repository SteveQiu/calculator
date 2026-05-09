package com.example.calculator.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.example.calculator.model.ThemeId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BillingRepository(private val context: Context) : PurchasesUpdatedListener {

    sealed class BillingResult {
        data class Success(val themeId: ThemeId) : BillingResult()
        data class Error(val message: String) : BillingResult()
        object Cancelled : BillingResult()
    }

    private val _purchaseResults = MutableSharedFlow<BillingResult>(extraBufferCapacity = 1)
    val purchaseResults: SharedFlow<BillingResult> = _purchaseResults

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        connectBillingClient()
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { restorePurchases() }
                }
            }
            override fun onBillingServiceDisconnected() {
                // Will retry on next purchase attempt
            }
        })
    }

    suspend fun queryProductDetails(themeId: ThemeId): ProductDetails? {
        val skuId = themeId.skuId ?: return null
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(skuId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        return withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params).productDetailsList?.firstOrNull()
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails, themeId: ThemeId) {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        result: com.android.billingclient.api.BillingResult,
        purchases: List<Purchase>?
    ) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseResults.tryEmit(BillingResult.Cancelled)
            }
            else -> {
                _purchaseResults.tryEmit(BillingResult.Error(result.debugMessage))
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params)
            }
            val themeId = ThemeId.entries.firstOrNull { theme ->
                theme.skuId != null && purchase.products.contains(theme.skuId)
            }
            if (themeId != null) {
                _purchaseResults.tryEmit(BillingResult.Success(themeId))
            }
        }
    }

    private suspend fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        result.purchasesList.forEach { handlePurchase(it) }
    }

    fun destroy() {
        scope.cancel()
        billingClient.endConnection()
    }
}