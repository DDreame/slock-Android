package com.slock.app.ui.machine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.data.model.Machine
import com.slock.app.data.model.MachineAgent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MachineCrudComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val connectedMachine = Machine(
        id = "m-1",
        name = "dev-workstation",
        status = "connected",
        runningAgents = emptyList()
    )

    private val blockedMachine = Machine(
        id = "m-2",
        name = "prod-server",
        status = "connected",
        runningAgents = listOf(
            MachineAgent(id = "a-1", name = "Agent1", status = "active"),
            MachineAgent(id = "a-2", name = "Agent2", status = "active")
        )
    )

    @Test
    fun `Add Machine button visible in machine list`() {
        composeTestRule.setContent {
            MachineListScreen(
                state = MachineUiState(machines = listOf(connectedMachine)),
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("+ Add Machine").assertExists()
    }

    @Test
    fun `Add Machine button triggers onAddMachine callback`() {
        var addCalled = false
        composeTestRule.setContent {
            MachineListScreen(
                state = MachineUiState(machines = listOf(connectedMachine)),
                onAddMachine = { addCalled = true },
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("+ Add Machine").performClick()
        composeTestRule.waitForIdle()
        assertTrue("onAddMachine must be called", addCalled)
    }

    @Test
    fun `Add Machine button visible in empty state`() {
        composeTestRule.setContent {
            MachineListScreen(
                state = MachineUiState(machines = emptyList()),
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("+ Add Machine").assertExists()
    }

    @Test
    fun `Step 1 Choose Type shows Your Computer and Cloud Sandbox`() {
        composeTestRule.setContent {
            MachineListScreen(
                state = MachineUiState(
                    machines = listOf(connectedMachine),
                    addMachineStep = AddMachineStep.ChooseType
                ),
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your Computer").assertExists()
        composeTestRule.onNodeWithText("COMING SOON").assertExists()
    }

    @Test
    fun `Step 2 Connecting shows waiting text and cancel`() {
        composeTestRule.setContent {
            MachineListScreen(
                state = MachineUiState(
                    machines = listOf(connectedMachine),
                    addMachineStep = AddMachineStep.Connecting,
                    newMachineApiKey = "sk-test-key"
                ),
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("等待 Machine 连接...", substring = true).assertExists()
        composeTestRule.onNodeWithText("COPY").assertExists()
    }

    @Test
    fun `Step 3 Connected shows success and finish button`() {
        val machine = Machine(id = "m-1", name = "dev-workstation", hostname = "my-host", os = "Linux")
        composeTestRule.setContent {
            MachineListScreen(
                state = MachineUiState(
                    machines = listOf(connectedMachine),
                    addMachineStep = AddMachineStep.Connected,
                    connectedMachine = machine,
                    newMachineName = "my-host"
                ),
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Machine 已连接!").assertExists()
        composeTestRule.onNodeWithText("完成").assertExists()
    }
}
