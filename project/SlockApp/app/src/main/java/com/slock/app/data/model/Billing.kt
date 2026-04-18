package com.slock.app.data.model

data class BillingSubscriptionResponse(
    val plan: String? = null,
    val tier: String? = null,
    val status: String? = null,
    val renewsAt: String? = null,
    val renewalDate: String? = null,
    val currentPeriodEnd: String? = null,
    val subscription: BillingSubscriptionDetails? = null
)

data class BillingSubscriptionDetails(
    val plan: String? = null,
    val tier: String? = null,
    val status: String? = null,
    val renewsAt: String? = null,
    val renewalDate: String? = null,
    val currentPeriodEnd: String? = null
)

data class BillingPlanSummary(
    val planName: String = "Free",
    val statusLabel: String = "Read-only preview",
    val renewalLabel: String = "Manage upgrades and billing changes on web for now.",
    val sourceLabel: String = "Android currently shows plan details only. Purchases and upgrades still happen on web."
)

fun BillingSubscriptionResponse.toBillingPlanSummary(): BillingPlanSummary {
    val details = subscription
    val planName = normalizeBillingLabel(
        details?.plan,
        details?.tier,
        plan,
        tier,
        fallback = "Free"
    )
    val statusLabel = normalizeBillingLabel(
        details?.status,
        status,
        fallback = "Read-only preview"
    )
    val renewalValue = firstNonBlank(
        details?.renewsAt,
        details?.renewalDate,
        details?.currentPeriodEnd,
        renewsAt,
        renewalDate,
        currentPeriodEnd
    )

    return BillingPlanSummary(
        planName = planName,
        statusLabel = statusLabel,
        renewalLabel = renewalValue
            ?.let { "Renews $it" }
            ?: "Manage upgrades and billing changes on web for now.",
        sourceLabel = if (renewalValue == null) {
            "Android currently shows plan details only. Purchases and upgrades still happen on web."
        } else {
            "Billing actions still happen on web. Android shows your current plan and renewal info."
        }
    )
}

private fun normalizeBillingLabel(vararg values: String?, fallback: String): String {
    val raw = firstNonBlank(*values) ?: return fallback
    return raw
        .replace('-', ' ')
        .replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase().replaceFirstChar { char -> char.titlecase() }
        }
        .ifBlank { fallback }
}

private fun firstNonBlank(vararg values: String?): String? =
    values.firstOrNull { !it.isNullOrBlank() }?.trim()
