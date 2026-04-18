package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.model.BillingPlanSummary
import com.slock.app.data.model.toBillingPlanSummary
import javax.inject.Inject

interface BillingRepository {
    suspend fun getSubscriptionSummary(): Result<BillingPlanSummary>
}

class BillingRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : BillingRepository {

    override suspend fun getSubscriptionSummary(): Result<BillingPlanSummary> {
        return try {
            val response = apiService.getBillingSubscription()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toBillingPlanSummary())
            } else {
                Result.failure(Exception("Get billing subscription failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
