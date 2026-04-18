package com.slock.app.ui.machine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MachineCrudSourceTest {

    private val apiSource: String = listOf(
        File("src/main/java/com/slock/app/data/api/ApiService.kt"),
        File("app/src/main/java/com/slock/app/data/api/ApiService.kt")
    ).first { it.exists() }.readText()

    private val repoSource: String = listOf(
        File("src/main/java/com/slock/app/data/repository/MachineRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/MachineRepository.kt")
    ).first { it.exists() }.readText()

    private val vmSource: String = listOf(
        File("src/main/java/com/slock/app/ui/machine/MachineViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/machine/MachineViewModel.kt")
    ).first { it.exists() }.readText()

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/machine/MachineListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/machine/MachineListScreen.kt")
    ).first { it.exists() }.readText()

    private val navSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    private val modelSource: String = listOf(
        File("src/main/java/com/slock/app/data/model/Agent.kt"),
        File("app/src/main/java/com/slock/app/data/model/Agent.kt")
    ).first { it.exists() }.readText()

    // --- Model layer ---

    @Test
    fun `Machine model has hostname field`() {
        assertTrue(
            "Machine must have hostname field",
            modelSource.contains("val hostname: String?")
        )
    }

    @Test
    fun `Machine model has os field`() {
        assertTrue(
            "Machine must have os field",
            modelSource.contains("val os: String?")
        )
    }

    @Test
    fun `Machine model has daemonVersion field`() {
        assertTrue(
            "Machine must have daemonVersion field",
            modelSource.contains("val daemonVersion: String?")
        )
    }

    @Test
    fun `Machine model has runtimes field`() {
        assertTrue(
            "Machine must have runtimes field",
            modelSource.contains("val runtimes:")
        )
    }

    @Test
    fun `CreateMachineRequest exists`() {
        assertTrue(
            "CreateMachineRequest must exist",
            modelSource.contains("data class CreateMachineRequest(")
        )
    }

    @Test
    fun `CreateMachineResponse exists`() {
        assertTrue(
            "CreateMachineResponse must exist",
            modelSource.contains("data class CreateMachineResponse(")
        )
    }

    @Test
    fun `RenameMachineRequest exists`() {
        assertTrue(
            "RenameMachineRequest must exist",
            modelSource.contains("data class RenameMachineRequest(")
        )
    }

    // --- API layer ---

    @Test
    fun `ApiService has createMachine endpoint`() {
        assertTrue(
            "ApiService must have createMachine",
            apiSource.contains("suspend fun createMachine(")
        )
    }

    @Test
    fun `ApiService createMachine uses POST method`() {
        val beforeMethod = apiSource.substringBefore("suspend fun createMachine(")
        val lastAnnotation = beforeMethod.trimEnd().lines().last()
        assertTrue(
            "createMachine must use @POST annotation",
            lastAnnotation.contains("@POST")
        )
    }

    @Test
    fun `ApiService has renameMachine endpoint`() {
        assertTrue(
            "ApiService must have renameMachine",
            apiSource.contains("suspend fun renameMachine(")
        )
    }

    @Test
    fun `ApiService renameMachine uses PATCH method`() {
        val beforeMethod = apiSource.substringBefore("suspend fun renameMachine(")
        val lastAnnotation = beforeMethod.trimEnd().lines().last()
        assertTrue(
            "renameMachine must use @PATCH annotation",
            lastAnnotation.contains("@PATCH")
        )
    }

    // --- Repository layer ---

    @Test
    fun `MachineRepository interface has createMachine`() {
        val interfaceBlock = repoSource
            .substringAfter("interface MachineRepository")
            .substringBefore("class MachineRepositoryImpl")
        assertTrue(
            "MachineRepository interface must declare createMachine",
            interfaceBlock.contains("suspend fun createMachine(")
        )
    }

    @Test
    fun `MachineRepository interface has renameMachine`() {
        val interfaceBlock = repoSource
            .substringAfter("interface MachineRepository")
            .substringBefore("class MachineRepositoryImpl")
        assertTrue(
            "MachineRepository interface must declare renameMachine",
            interfaceBlock.contains("suspend fun renameMachine(")
        )
    }

    @Test
    fun `MachineRepositoryImpl implements createMachine`() {
        val implBlock = repoSource.substringAfter("class MachineRepositoryImpl")
        assertTrue(
            "MachineRepositoryImpl must implement createMachine",
            implBlock.contains("override suspend fun createMachine(")
        )
    }

    @Test
    fun `MachineRepositoryImpl implements renameMachine`() {
        val implBlock = repoSource.substringAfter("class MachineRepositoryImpl")
        assertTrue(
            "MachineRepositoryImpl must implement renameMachine",
            implBlock.contains("override suspend fun renameMachine(")
        )
    }

    // --- ViewModel layer ---

    @Test
    fun `MachineViewModel has startAddMachine method`() {
        assertTrue(
            "MachineViewModel must have startAddMachine",
            vmSource.contains("fun startAddMachine(")
        )
    }

    @Test
    fun `MachineViewModel has createMachine method`() {
        assertTrue(
            "MachineViewModel must have createMachine",
            vmSource.contains("fun createMachine(")
        )
    }

    @Test
    fun `MachineViewModel has finishAddMachine method`() {
        assertTrue(
            "MachineViewModel must have finishAddMachine",
            vmSource.contains("fun finishAddMachine(")
        )
    }

    @Test
    fun `MachineViewModel has cancelAddMachine method`() {
        assertTrue(
            "MachineViewModel must have cancelAddMachine",
            vmSource.contains("fun cancelAddMachine(")
        )
    }

    @Test
    fun `MachineViewModel has requestDeleteMachine method`() {
        assertTrue(
            "MachineViewModel must have requestDeleteMachine",
            vmSource.contains("fun requestDeleteMachine(")
        )
    }

    @Test
    fun `requestDeleteMachine checks runningAgents`() {
        val block = vmSource.substringAfter("fun requestDeleteMachine(")
            .substringBefore("fun dismissDeleteBlocked(")
        assertTrue(
            "requestDeleteMachine must check runningAgents",
            block.contains("runningAgents")
        )
    }

    @Test
    fun `MachineViewModel state has addMachineStep`() {
        assertTrue(
            "MachineUiState must have addMachineStep",
            vmSource.contains("addMachineStep")
        )
    }

    @Test
    fun `MachineViewModel state has deleteBlockedMachine`() {
        assertTrue(
            "MachineUiState must have deleteBlockedMachine",
            vmSource.contains("deleteBlockedMachine")
        )
    }

    @Test
    fun `MachineViewModel has polling for waiting`() {
        assertTrue(
            "MachineViewModel must have startPolling for waiting state",
            vmSource.contains("startPolling")
        )
    }

    @Test
    fun `createMachine calls repository createMachine`() {
        val block = vmSource.substringAfter("fun createMachine(")
            .substringBefore("fun finishAddMachine(")
        assertTrue(
            "createMachine must call machineRepository.createMachine",
            block.contains("machineRepository.createMachine")
        )
    }

    @Test
    fun `finishAddMachine calls renameMachine on repository`() {
        val block = vmSource.substringAfter("fun finishAddMachine(")
            .substringBefore("fun cancelAddMachine(")
        assertTrue(
            "finishAddMachine must call machineRepository.renameMachine",
            block.contains("machineRepository.renameMachine")
        )
    }

    // --- Screen layer ---

    @Test
    fun `MachineListScreen has onAddMachine callback`() {
        assertTrue(
            "MachineListScreen must have onAddMachine callback",
            screenSource.contains("onAddMachine")
        )
    }

    @Test
    fun `MachineListScreen has onCreateMachine callback`() {
        assertTrue(
            "MachineListScreen must have onCreateMachine callback",
            screenSource.contains("onCreateMachine")
        )
    }

    @Test
    fun `MachineListScreen has onFinishAddMachine callback`() {
        assertTrue(
            "MachineListScreen must have onFinishAddMachine callback",
            screenSource.contains("onFinishAddMachine")
        )
    }

    @Test
    fun `MachineListScreen has AddMachineButton`() {
        assertTrue(
            "MachineListScreen must have AddMachineButton composable",
            screenSource.contains("AddMachineButton")
        )
    }

    @Test
    fun `MachineListScreen renders Add Machine text`() {
        assertTrue(
            "MachineListScreen must render '+ Add Machine' text",
            screenSource.contains("+ Add Machine")
        )
    }

    @Test
    fun `MachineListScreen has ChooseTypeContent`() {
        assertTrue(
            "MachineListScreen must have ChooseTypeContent for step 1",
            screenSource.contains("ChooseTypeContent")
        )
    }

    @Test
    fun `MachineListScreen has ConnectingContent`() {
        assertTrue(
            "MachineListScreen must have ConnectingContent for step 2",
            screenSource.contains("ConnectingContent")
        )
    }

    @Test
    fun `MachineListScreen has ConnectedContent`() {
        assertTrue(
            "MachineListScreen must have ConnectedContent for step 3",
            screenSource.contains("ConnectedContent")
        )
    }

    @Test
    fun `MachineListScreen has DeleteBlockedDialog`() {
        assertTrue(
            "MachineListScreen must have DeleteBlockedDialog",
            screenSource.contains("DeleteBlockedDialog")
        )
    }

    @Test
    fun `Screen shows Cloud Sandbox Coming Soon`() {
        assertTrue(
            "ChooseType must show Coming Soon for Cloud Sandbox",
            screenSource.contains("COMING SOON")
        )
    }

    @Test
    fun `Screen shows waiting text`() {
        assertTrue(
            "ConnectingContent must show waiting text",
            screenSource.contains("等待 Machine 连接")
        )
    }

    @Test
    fun `Screen shows connected success text`() {
        assertTrue(
            "ConnectedContent must show Machine 已连接!",
            screenSource.contains("Machine 已连接!")
        )
    }

    // --- NavHost wiring ---

    @Test
    fun `NavHost wires onAddMachine to viewModel`() {
        val machineBlock = navSource
            .substringAfter("MachineListScreen(")
        assertTrue(
            "NavHost must wire onAddMachine",
            machineBlock.contains("onAddMachine")
        )
    }

    @Test
    fun `NavHost wires onCreateMachine to viewModel`() {
        val machineBlock = navSource
            .substringAfter("MachineListScreen(")
        assertTrue(
            "NavHost must wire onCreateMachine",
            machineBlock.contains("onCreateMachine")
        )
    }

    @Test
    fun `NavHost wires onFinishAddMachine to viewModel`() {
        val machineBlock = navSource
            .substringAfter("MachineListScreen(")
        assertTrue(
            "NavHost must wire onFinishAddMachine",
            machineBlock.contains("onFinishAddMachine")
        )
    }

    @Test
    fun `NavHost wires onConfirmDelete to viewModel`() {
        val machineBlock = navSource
            .substringAfter("MachineListScreen(")
        assertTrue(
            "NavHost must wire onConfirmDelete",
            machineBlock.contains("onConfirmDelete")
        )
    }

    @Test
    fun `NavHost wires requestDeleteMachine for delete guard`() {
        val machineBlock = navSource
            .substringAfter("MachineListScreen(")
        assertTrue(
            "NavHost must wire requestDeleteMachine for delete guard",
            machineBlock.contains("requestDeleteMachine")
        )
    }

    // --- Preservation ---

    @Test
    fun `existing deleteMachine method preserved`() {
        assertTrue(
            "deleteMachine must still exist",
            vmSource.contains("fun deleteMachine(")
        )
    }

    @Test
    fun `existing loadMachines method preserved`() {
        assertTrue(
            "loadMachines must still exist",
            vmSource.contains("fun loadMachines(")
        )
    }

    @Test
    fun `existing MachineCard preserved`() {
        assertTrue(
            "MachineCard must still exist",
            screenSource.contains("MachineCard")
        )
    }
}
