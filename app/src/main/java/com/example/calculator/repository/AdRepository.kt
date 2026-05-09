package com.example.calculator.repository

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AdRepository(private val context: Context) {

    sealed class AdResult {
        object Rewarded : AdResult()
        data class Error(val message: String) : AdResult()
        object Dismissed : AdResult()
    }

    private val _adResults = MutableSharedFlow<AdResult>(extraBufferCapacity = 1)
    val adResults: SharedFlow<AdResult> = _adResults

    // Test ad unit ID — replace with real ID for production
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    init {
        try {
            MobileAds.initialize(context) {
                loadAd()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                isLoading = false
                setupFullScreenCallback()
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
                isLoading = false
            }
        })
    }

    private fun setupFullScreenCallback() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadAd()
                _adResults.tryEmit(AdResult.Error(error.message))
            }
        }
    }

    fun isAdReady(): Boolean = rewardedAd != null

    fun showAd(activity: Activity) {
        val ad = rewardedAd
        if (ad == null) {
            _adResults.tryEmit(AdResult.Error("Ad not ready"))
            return
        }
        ad.show(activity) { _ ->
            _adResults.tryEmit(AdResult.Rewarded)
        }
    }
}