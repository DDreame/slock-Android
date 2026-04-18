package com.slock.app.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.model.BillingPlanSummary
import com.slock.app.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingPlansUiState(
    val isLoading: Boolean = true,
    val planSummary: BillingPlanSummary = BillingPlanSummary(),
    val notice: String = "Android currently shows your plan details only. Billing changes still happen on web.",
    val error: String? = null
)

@HiltViewModel
class BillingPlansViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BillingPlansUiState())
    val state: StateFlow<BillingPlansUiState> = _state.asStateFlow()

    init {
        loadBillingPlan()
    }

    fun loadBillingPlan() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }
            billingRepository.getSubscriptionSummary().fold(
                onSuccess = { summary ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            planSummary = summary,
                            notice = summary.sourceLabel,
                            error = null
                        )
                    }
                },
                onFailure = {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            notice = "Live billing details are temporarily unavailable. Android still shows a read-only plan placeholder.",
                            error = it.error ?: "Unable to refresh billing details"
                        )
                    }
                }
            )
        }
    }
}
