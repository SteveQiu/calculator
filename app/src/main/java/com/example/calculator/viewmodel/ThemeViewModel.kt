package com.example.calculator.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.model.ThemeId
import com.example.calculator.repository.AdRepository
import com.example.calculator.repository.BillingRepository
import com.example.calculator.repository.ThemeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val themeRepository: ThemeRepository,
    private val billingRepository: BillingRepository,
    private val adRepository: AdRepository
) : ViewModel() {

    val activeTheme: StateFlow<ThemeId> = themeRepository.activeThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeId.CLASSIC)

    val unlockedThemes: StateFlow<Set<ThemeId>> = themeRepository.unlockedThemesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    sealed class UiEvent {
        data class ThemeUnlocked(val themeId: ThemeId) : UiEvent()
        data class Error(val message: String) : UiEvent()
        object AdNotReady : UiEvent()
        object PurchaseCancelled : UiEvent()
    }

    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    /** Set before calling watchAdToUnlock so the ad callback knows which theme to unlock. */
    var pendingUnlockTheme: ThemeId? = null

    init {
        observeBillingResults()
        observeAdResults()
    }

    fun selectTheme(themeId: ThemeId) {
        viewModelScope.launch {
            if (themeRepository.isThemeUnlocked(themeId, unlockedThemes.value)) {
                themeRepository.setActiveTheme(themeId)
            }
        }
    }

    fun isThemeUnlocked(themeId: ThemeId): Boolean =
        themeRepository.isThemeUnlocked(themeId, unlockedThemes.value)

    fun watchAdToUnlock(activity: Activity) {
        if (!adRepository.isAdReady()) {
            adRepository.loadAd()
            _uiEvents.tryEmit(UiEvent.AdNotReady)
            return
        }
        adRepository.showAd(activity)
    }

    fun buyTheme(activity: Activity, themeId: ThemeId) {
        viewModelScope.launch {
            val productDetails = billingRepository.queryProductDetails(themeId)
            if (productDetails != null) {
                billingRepository.launchPurchaseFlow(activity, productDetails, themeId)
            } else {
                _uiEvents.tryEmit(UiEvent.Error("Product not found. Check your internet connection."))
            }
        }
    }

    private fun observeBillingResults() {
        viewModelScope.launch {
            billingRepository.purchaseResults.collect { result ->
                when (result) {
                    is BillingRepository.BillingResult.Success -> {
                        themeRepository.unlockTheme(result.themeId)
                        themeRepository.setActiveTheme(result.themeId)
                        _uiEvents.emit(UiEvent.ThemeUnlocked(result.themeId))
                    }
                    is BillingRepository.BillingResult.Error -> {
                        _uiEvents.emit(UiEvent.Error(result.message))
                    }
                    BillingRepository.BillingResult.Cancelled -> {
                        _uiEvents.emit(UiEvent.PurchaseCancelled)
                    }
                }
            }
        }
    }

    private fun observeAdResults() {
        viewModelScope.launch {
            adRepository.adResults.collect { result ->
                when (result) {
                    AdRepository.AdResult.Rewarded -> {
                        pendingUnlockTheme?.let { themeId ->
                            themeRepository.unlockTheme(themeId)
                            themeRepository.setActiveTheme(themeId)
                            _uiEvents.emit(UiEvent.ThemeUnlocked(themeId))
                            pendingUnlockTheme = null
                        }
                    }
                    is AdRepository.AdResult.Error -> {
                        _uiEvents.emit(UiEvent.Error(result.message))
                    }
                    AdRepository.AdResult.Dismissed -> { /* no-op */ }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingRepository.destroy()
    }
}