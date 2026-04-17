package com.slock.app.integration

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.slock.app.di.DatabaseModule
import com.slock.app.di.NetworkModule
import com.slock.app.di.RepositoryModule
import com.slock.app.ui.theme.NeoButton
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(RobolectricTestRunner::class)
@dagger.hilt.android.testing.UninstallModules(
    NetworkModule::class,
    DatabaseModule::class,
    RepositoryModule::class
)
class HiltComposeIntegrationSampleTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `hilt bootstraps with robolectric and compose test rule`() {
        composeTestRule.setContent {
            Text("Hilt + Robolectric + Compose")
        }
        composeTestRule.onNodeWithText("Hilt + Robolectric + Compose").assertIsDisplayed()
    }

    @Test
    fun `renders production NeoButton composable under hilt test`() {
        composeTestRule.setContent {
            NeoButton(text = "Integration Test", onClick = {})
        }
        composeTestRule.onNodeWithText("Integration Test").assertIsDisplayed()
    }
}
