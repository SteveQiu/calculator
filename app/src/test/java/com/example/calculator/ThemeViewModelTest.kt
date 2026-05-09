package com.example.calculator

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.calculator.model.ThemeId
import com.example.calculator.repository.AdRepository
import com.example.calculator.repository.BillingRepository
import com.example.calculator.repository.ThemeRepository
import com.example.calculator.viewmodel.ThemeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class ThemeViewModelTest {

    @get:Rule val instantExecutor = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var themeRepository: ThemeRepository
    private lateinit var billingRepository: BillingRepository
    private lateinit var adRepository: AdRepository
    private lateinit var vm: ThemeViewModel

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        themeRepository = mock()
        billingRepository = mock()
        adRepository = mock()

        whenever(themeRepository.activeThemeFlow).thenReturn(flowOf(ThemeId.CLASSIC))
        whenever(themeRepository.unlockedThemesFlow).thenReturn(flowOf(setOf(ThemeId.CLASSIC)))
        whenever(billingRepository.purchaseResults).thenReturn(MutableSharedFlow())
        whenever(adRepository.adResults).thenReturn(MutableSharedFlow())

        vm = ThemeViewModel(themeRepository, billingRepository, adRepository)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `selectTheme calls setActiveTheme when unlocked`() = runTest {
        whenever(themeRepository.isThemeUnlocked(ThemeId.CLASSIC, setOf(ThemeId.CLASSIC))).thenReturn(true)
        vm.selectTheme(ThemeId.CLASSIC)
        advanceUntilIdle()
        verify(themeRepository).setActiveTheme(ThemeId.CLASSIC)
    }

    @Test fun `selectTheme does NOT call setActiveTheme when locked`() = runTest {
        whenever(themeRepository.isThemeUnlocked(ThemeId.MIDNIGHT, setOf(ThemeId.CLASSIC))).thenReturn(false)
        vm.selectTheme(ThemeId.MIDNIGHT)
        advanceUntilIdle()
        verify(themeRepository, never()).setActiveTheme(ThemeId.MIDNIGHT)
    }

    @Test fun `watchAdToUnlock emits AdNotReady when ad not loaded`() = runTest {
        whenever(adRepository.isAdReady()).thenReturn(false)
        val events = mutableListOf<ThemeViewModel.UiEvent>()
        val job = launch { vm.uiEvents.collect { events.add(it) } }

        vm.watchAdToUnlock(mock())
        advanceUntilIdle()

        assertTrue(events.any { it is ThemeViewModel.UiEvent.AdNotReady })
        job.cancel()
    }

    @Test fun `billing Success unlocks theme and sets active`() = runTest {
        val purchaseFlow = MutableSharedFlow<BillingRepository.BillingResult>(extraBufferCapacity = 1)
        whenever(billingRepository.purchaseResults).thenReturn(purchaseFlow)

        vm = ThemeViewModel(themeRepository, billingRepository, adRepository)

        val events = mutableListOf<ThemeViewModel.UiEvent>()
        val job = launch { vm.uiEvents.collect { events.add(it) } }

        purchaseFlow.emit(BillingRepository.BillingResult.Success(ThemeId.MIDNIGHT))
        advanceUntilIdle()

        verify(themeRepository).unlockTheme(ThemeId.MIDNIGHT)
        verify(themeRepository).setActiveTheme(ThemeId.MIDNIGHT)
        assertTrue(events.any { it is ThemeViewModel.UiEvent.ThemeUnlocked })
        job.cancel()
    }

    @Test fun `billing Error emits Error event`() = runTest {
        val purchaseFlow = MutableSharedFlow<BillingRepository.BillingResult>(extraBufferCapacity = 1)
        whenever(billingRepository.purchaseResults).thenReturn(purchaseFlow)

        vm = ThemeViewModel(themeRepository, billingRepository, adRepository)

        val events = mutableListOf<ThemeViewModel.UiEvent>()
        val job = launch { vm.uiEvents.collect { events.add(it) } }

        purchaseFlow.emit(BillingRepository.BillingResult.Error("Payment failed"))
        advanceUntilIdle()

        assertTrue(events.any { it is ThemeViewModel.UiEvent.Error })
        job.cancel()
    }

    @Test fun `billing Cancelled emits PurchaseCancelled event`() = runTest {
        val purchaseFlow = MutableSharedFlow<BillingRepository.BillingResult>(extraBufferCapacity = 1)
        whenever(billingRepository.purchaseResults).thenReturn(purchaseFlow)

        vm = ThemeViewModel(themeRepository, billingRepository, adRepository)

        val events = mutableListOf<ThemeViewModel.UiEvent>()
        val job = launch { vm.uiEvents.collect { events.add(it) } }

        purchaseFlow.emit(BillingRepository.BillingResult.Cancelled)
        advanceUntilIdle()

        assertTrue(events.any { it is ThemeViewModel.UiEvent.PurchaseCancelled })
        job.cancel()
    }

    @Test fun `ad Rewarded with pending theme unlocks and sets active`() = runTest {
        val adFlow = MutableSharedFlow<AdRepository.AdResult>(extraBufferCapacity = 1)
        whenever(adRepository.adResults).thenReturn(adFlow)

        vm = ThemeViewModel(themeRepository, billingRepository, adRepository)
        vm.pendingUnlockTheme = ThemeId.OCEAN

        val events = mutableListOf<ThemeViewModel.UiEvent>()
        val job = launch { vm.uiEvents.collect { events.add(it) } }

        adFlow.emit(AdRepository.AdResult.Rewarded)
        advanceUntilIdle()

        verify(themeRepository).unlockTheme(ThemeId.OCEAN)
        verify(themeRepository).setActiveTheme(ThemeId.OCEAN)
        assertNull(vm.pendingUnlockTheme)
        assertTrue(events.any { it is ThemeViewModel.UiEvent.ThemeUnlocked })
        job.cancel()
    }

    @Test fun `ad Rewarded with no pending theme does nothing`() = runTest {
        val adFlow = MutableSharedFlow<AdRepository.AdResult>(extraBufferCapacity = 1)
        whenever(adRepository.adResults).thenReturn(adFlow)

        vm = ThemeViewModel(themeRepository, billingRepository, adRepository)
        vm.pendingUnlockTheme = null

        adFlow.emit(AdRepository.AdResult.Rewarded)
        advanceUntilIdle()

        verify(themeRepository, never()).unlockTheme(any())
    }
}
