package com.example.calculator.repository

import android.app.Activity
import android.content.Context
import com.example.calculator.model.ThemeId
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Wraps AdMob RewardedAd lifecycle for theme unlock rewards.
 *
 * One [RewardedAd] instance is preloaded per session. After it is shown, a new ad is preloaded
 * immediately so the next unlock request is fast.
 *
 * Ad Unit ID: use the AdMob test ID during development.
 *   Test ID: ca-app-pub-3940256099942544/5224354917
 *
 * Flow:
 *   1. [preloadAd] called on app start (from AppModule / CalculatorApp).
 *   2. [showRewardedAd] called by ThemeViewModel when user taps "Watch Ad".
 *   3. On reward earned → [onRewarded] lambda fires → ThemeRepository.unlockTheme() called.
 *   4. Ad dismissed → [preloadAd] called again to prepare the next ad.
 *
 * TODO: Implement preloadAd() — RewardedAd.load(context, AD_UNIT_ID, AdRequest, callback)
 * TODO: Implement showRewardedAd(activity, themeId, onRewarded) — show loaded ad or log not ready
 * TODO: Handle load failures gracefully (isAdReady flag, retry with back-off)
 * TODO: Implement isAdReady(): Boolean so ThemeViewModel can disable "Watch Ad" when ad isn't loaded
 */
class AdRepository(private val context: Context) {

    private var rewardedAd: RewardedAd? = null

    companion object {
        // Replace with your real AdMob Rewarded Ad unit ID before release.
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    fun preloadAd() {
        // TODO: RewardedAd.load(context, AD_UNIT_ID, AdRequest.Builder().build(), loadCallback)
    }

    fun showRewardedAd(activity: Activity, themeId: ThemeId, onRewarded: (ThemeId) -> Unit) {
        // TODO: Check rewardedAd != null; show or notify user ad isn't ready
        // TODO: Set FullScreenContentCallback to call preloadAd() on dismiss/failure
        // TODO: Set OnUserEarnedRewardListener to call onRewarded(themeId)
    }

    fun isAdReady(): Boolean = rewardedAd != null
}
