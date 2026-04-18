package com.slock.app.ui.billing

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingPlansNavHostSourceTest {

    private val navHostSource = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    private val settingsSource = listOf(
        File("src/main/java/com/slock/app/ui/settings/SettingsScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/settings/SettingsScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `Routes exposes billing plans route`() {
        assertTrue(navHostSource.contains("const val BILLING_PLANS = \"billing_plans\""))
    }

    @Test
    fun `NavHost wires settings entry to billing plans screen`() {
        assertTrue(navHostSource.contains("onOpenBillingPlans = { navController.navigate(Routes.BILLING_PLANS) }"))
        assertTrue(navHostSource.contains("composable(Routes.BILLING_PLANS)"))
        assertTrue(navHostSource.contains("BillingPlansScreen("))
    }

    @Test
    fun `Settings screen renders billing plans entry button`() {
        assertTrue(settingsSource.contains("SettingsInfoCard("))
        assertTrue(settingsSource.contains("title = \"Billing / Plans\""))
        assertTrue(settingsSource.contains("text = \"VIEW BILLING / PLANS\""))
    }
}
