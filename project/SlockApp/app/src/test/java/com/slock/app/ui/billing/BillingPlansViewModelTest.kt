package com.slock.app.ui.billing

import com.slock.app.data.model.BillingPlanSummary
import com.slock.app.data.repository.BillingRepository
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BillingPlansViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val billingRepository: BillingRepository = mock()

    @Test
    fun `loadBillingPlan maps subscription summary into ui state`() = runTest {
        whenever(billingRepository.getSubscriptionSummary()).thenReturn(
            Result.success(
                BillingPlanSummary(
                    planName = "Pro",
                    statusLabel = "Active",
                    renewalLabel = "Renews 2026-05-01",
                    sourceLabel = "Billing actions still happen on web. Android shows your current plan and renewal info."
                )
            )
        )

        val viewModel = BillingPlansViewModel(billingRepository)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Pro", viewModel.state.value.planSummary.planName)
        assertEquals("Active", viewModel.state.value.planSummary.statusLabel)
        assertEquals("Renews 2026-05-01", viewModel.state.value.planSummary.renewalLabel)
    }

    @Test
    fun `loadBillingPlan keeps placeholder when billing fetch fails`() = runTest {
        whenever(billingRepository.getSubscriptionSummary()).thenReturn(
            Result.failure(IllegalStateException("billing down"))
        )

        val viewModel = BillingPlansViewModel(billingRepository)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Free", viewModel.state.value.planSummary.planName)
        assertEquals(
            "Live billing details are temporarily unavailable. Android still shows a read-only plan placeholder.",
            viewModel.state.value.notice
        )
        assertEquals("Unable to refresh billing details", viewModel.state.value.error)
    }
}
