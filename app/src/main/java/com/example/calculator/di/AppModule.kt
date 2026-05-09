package com.example.calculator.di

import android.content.Context
import com.example.calculator.repository.AdRepository
import com.example.calculator.repository.BillingRepository
import com.example.calculator.repository.ThemeRepository

object AppModule {
    private var themeRepository: ThemeRepository? = null
    private var billingRepository: BillingRepository? = null
    private var adRepository: AdRepository? = null

    fun provideThemeRepository(context: Context): ThemeRepository {
        return themeRepository ?: ThemeRepository(context.applicationContext).also { themeRepository = it }
    }

    fun provideBillingRepository(context: Context): BillingRepository {
        return billingRepository ?: BillingRepository(context.applicationContext).also { billingRepository = it }
    }

    fun provideAdRepository(context: Context): AdRepository {
        return adRepository ?: AdRepository(context.applicationContext).also { adRepository = it }
    }
}