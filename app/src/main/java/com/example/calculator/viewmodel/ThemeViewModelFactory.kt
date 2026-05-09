package com.example.calculator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.repository.AdRepository
import com.example.calculator.repository.BillingRepository
import com.example.calculator.repository.ThemeRepository

class ThemeViewModelFactory(
    private val themeRepository: ThemeRepository,
    private val billingRepository: BillingRepository,
    private val adRepository: AdRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ThemeViewModel(themeRepository, billingRepository, adRepository) as T
}