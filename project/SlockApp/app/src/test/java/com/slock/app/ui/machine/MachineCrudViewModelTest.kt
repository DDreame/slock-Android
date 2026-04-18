package com.slock.app.ui.machine

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.CreateMachineResponse
import com.slock.app.data.model.Machine
import com.slock.app.data.model.MachineAgent
import com.slock.app.data.repository.MachineRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MachineCrudViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val machineRepository: MachineRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val secureTokenStorage: SecureTokenStorage = mock()
    private val activeServerHolder = ActiveServerHolder(secureTokenStorage)

    private fun createViewModel(): MachineViewModel {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        return MachineViewModel(
            machineRepository = machineRepository,
            socketIOManager = socketIOManager,
            activeServerHolder = activeServerHolder
        )
    }

    @Test
    fun `startAddMachine sets step to ChooseType`() = runTest {
        val viewModel = createViewModel()
        viewModel.startAddMachine()
        assertEquals(AddMachineStep.ChooseType, viewModel.state.value.addMachineStep)
    }

    @Test
    fun `createMachine transitions to Connecting with apiKey`() = runTest {
        val machine = Machine(id = "m-1", name = "test-machine")
        val response = CreateMachineResponse(machine = machine, apiKey = "sk-test-key")
        whenever(machineRepository.createMachine(any(), any())).thenReturn(Result.success(response))
        whenever(machineRepository.getMachines(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadMachines("server-1")
        advanceUntilIdle()

        viewModel.createMachine("test-machine")
        advanceUntilIdle()

        assertEquals(AddMachineStep.Connecting, viewModel.state.value.addMachineStep)
        assertEquals("sk-test-key", viewModel.state.value.newMachineApiKey)
        assertEquals("m-1", viewModel.state.value.newMachineId)
    }

    @Test
    fun `createMachine failure sets actionFeedback`() = runTest {
        whenever(machineRepository.createMachine(any(), any())).thenReturn(
            Result.failure(Exception("server error"))
        )
        whenever(machineRepository.getMachines(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadMachines("server-1")
        advanceUntilIdle()

        viewModel.createMachine("test-machine")
        advanceUntilIdle()

        assertEquals("Create machine failed: server error", viewModel.state.value.actionFeedback)
        assertNull(viewModel.state.value.addMachineStep)
    }

    @Test
    fun `cancelAddMachine clears add state`() = runTest {
        val viewModel = createViewModel()
        viewModel.startAddMachine()
        assertEquals(AddMachineStep.ChooseType, viewModel.state.value.addMachineStep)

        viewModel.cancelAddMachine()
        assertNull(viewModel.state.value.addMachineStep)
        assertNull(viewModel.state.value.newMachineApiKey)
    }

    @Test
    fun `requestDeleteMachine with no agents calls delete`() = runTest {
        val machine = Machine(id = "m-1", name = "test", runningAgents = emptyList())
        whenever(machineRepository.getMachines(any())).thenReturn(Result.success(listOf(machine)))
        whenever(machineRepository.deleteMachine(any(), any())).thenReturn(Result.success(Unit))

        val viewModel = createViewModel()
        viewModel.loadMachines("server-1")
        advanceUntilIdle()

        viewModel.requestDeleteMachine(machine)
        advanceUntilIdle()

        assertNull(viewModel.state.value.deleteBlockedMachine)
        assertEquals(0, viewModel.state.value.machines.size)
        verify(machineRepository).deleteMachine("server-1", "m-1")
    }

    @Test
    fun `requestDeleteMachine with agents sets deleteBlockedMachine`() = runTest {
        val agents = listOf(MachineAgent(id = "a-1", name = "Agent1", status = "active"))
        val machine = Machine(id = "m-1", name = "test", runningAgents = agents)
        whenever(machineRepository.getMachines(any())).thenReturn(Result.success(listOf(machine)))

        val viewModel = createViewModel()
        viewModel.loadMachines("server-1")
        advanceUntilIdle()

        viewModel.requestDeleteMachine(machine)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.deleteBlockedMachine)
        assertEquals("m-1", viewModel.state.value.deleteBlockedMachine?.id)
        assertEquals(1, viewModel.state.value.machines.size)
    }

    @Test
    fun `dismissDeleteBlocked clears blocked state`() = runTest {
        val agents = listOf(MachineAgent(id = "a-1", name = "Agent1", status = "active"))
        val machine = Machine(id = "m-1", name = "test", runningAgents = agents)

        val viewModel = createViewModel()
        viewModel.requestDeleteMachine(machine)
        assertNotNull(viewModel.state.value.deleteBlockedMachine)

        viewModel.dismissDeleteBlocked()
        assertNull(viewModel.state.value.deleteBlockedMachine)
    }

    @Test
    fun `deleteMachine failure sets actionFeedback`() = runTest {
        val machine = Machine(id = "m-1", name = "test", runningAgents = emptyList())
        whenever(machineRepository.getMachines(any())).thenReturn(Result.success(listOf(machine)))
        whenever(machineRepository.deleteMachine(any(), any())).thenReturn(
            Result.failure(Exception("network error"))
        )

        val viewModel = createViewModel()
        viewModel.loadMachines("server-1")
        advanceUntilIdle()

        viewModel.deleteMachine("m-1")
        advanceUntilIdle()

        assertEquals("Delete failed: network error", viewModel.state.value.actionFeedback)
        assertEquals(1, viewModel.state.value.machines.size)
    }

    @Test
    fun `finishAddMachine with new name calls renameMachine`() = runTest {
        val machine = Machine(id = "m-1", name = "original", hostname = "my-host")
        val connectedMachine = machine.copy(status = "connected")
        val createResponse = CreateMachineResponse(machine = machine, apiKey = "sk-key")
        whenever(machineRepository.createMachine(any(), any())).thenReturn(Result.success(createResponse))
        whenever(machineRepository.getMachines(any())).thenReturn(Result.success(listOf(connectedMachine)))
        whenever(machineRepository.renameMachine(any(), any(), any())).thenReturn(Result.success(connectedMachine.copy(name = "new-name")))

        val viewModel = createViewModel()
        viewModel.loadMachines("server-1")
        advanceUntilIdle()

        viewModel.createMachine("original")
        advanceUntilIdle()

        assertEquals("m-1", viewModel.state.value.newMachineId)

        viewModel.finishAddMachine("new-name")
        advanceUntilIdle()

        verify(machineRepository).renameMachine("server-1", "m-1", "new-name")
        assertNull(viewModel.state.value.addMachineStep)
    }
}
