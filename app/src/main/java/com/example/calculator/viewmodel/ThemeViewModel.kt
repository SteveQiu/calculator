package com.example.calculator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.model.Theme
import com.example.calculator.model.ThemeId
import com.example.calculator.repository.AdRepository
import com.example.calculator.repository.BillingRepository
import com.example.calculator.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Coordinates theme selection and unlock flows.
 *
 * Consumers observe:
 *   - [themeList]     — full list of Theme objects with current unlock state merged in
 *   - [activeThemeId] — the currently applied theme
 *   - [unlockRequest] — one-shot event signalling ThemePickerActivity to show ThemeUnlockDialog
 *
 * TODO: Combine ThemeRepository.unlockStateFlow with THEME_CATALOG to produce themeList
 * TODO: Implement requestUnlock(themeId) — emit unlockRequest event
 * TODO: Implement unlockWithAd(themeId, activity) — delegate to AdRepository
 * TODO: Implement unlockWithPurchase(themeId, activity) — delegate to BillingRepository
 * TODO: Implement setActiveTheme(themeId) — persist via ThemeRepository
 */
class ThemeViewModel(
    private val themeRepository: ThemeRepository,
    private val billingRepository: BillingRepository,
    private val adRepository: AdRepository,
) : ViewModel() {

    /** The theme the user has selected and that is currently applied. */
    val activeThemeId: StateFlow<ThemeId> = themeRepository.activeThemeIdFlow

    /** Map of ThemeId → isUnlocked driven by DataStore. */
    val unlockState: StateFlow<Map<ThemeId, Boolean>> = themeRepository.unlockStateFlow

    /**
     * One-shot event: ThemePickerActivity listens and shows ThemeUnlockDialog for the given ThemeId.
     */
    private val _unlockRequest = MutableSharedFlow<ThemeId>()
    val unlockRequest: SharedFlow<ThemeId> = _unlockRequest

    // TODO: val themeList: StateFlow<List<Theme>> — combine unlockState with THEME_CATALOG

    fun requestUnlock(themeId: ThemeId) {
        viewModelScope.launch { _unlockRequest.emit(themeId) }
    }

    fun setActiveTheme(themeId: ThemeId) {
        viewModelScope.launch { themeRepository.setActiveTheme(themeId) }
    }

    // TODO: fun unlockWithAd(themeId: ThemeId, activity: Activity)
    // TODO: fun unlockWithPurchase(themeId: ThemeId, activity: Activity)
}
