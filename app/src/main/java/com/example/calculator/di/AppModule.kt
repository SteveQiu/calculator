package com.example.calculator.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.repository.AdRepository
import com.example.calculator.repository.BillingRepository
import com.example.calculator.repository.ThemeRepository
import com.example.calculator.viewmodel.ThemeViewModel

/**
 * Manual DI module — no Hilt/Dagger at this app scale.
 *
 * All repositories are singletons held here. Activities obtain ViewModels
 * via [themeViewModelFactory] rather than injecting directly, keeping
 * the ViewModel layer decoupled from construction details.
 *
 * Initialisation order (called from CalculatorApp.onCreate):
 *   1. AppModule.init(applicationContext)
 *   2. adRepository.preloadAd()
 *   3. billingRepository.connect()
 */
object AppModule {

    private lateinit var appContext: Context

    val themeRepository: ThemeRepository by lazy { ThemeRepository(appContext) }
    val adRepository: AdRepository by lazy { AdRepository(appContext) }
    val billingRepository: BillingRepository by lazy {
        BillingRepository(appContext, themeRepository)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Factory for ThemeViewModel; call from Activity.viewModels { } delegate. */
    fun themeViewModelFactory(context: Context): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == ThemeViewModel::class.java)
                return ThemeViewModel(themeRepository, billingRepository, adRepository) as T
            }
        }
}
