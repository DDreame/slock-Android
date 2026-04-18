package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.CreateMachineRequest
import com.slock.app.data.model.CreateMachineResponse
import com.slock.app.data.model.Machine
import com.slock.app.data.model.RenameMachineRequest
import android.util.Log
import javax.inject.Inject

interface MachineRepository {
    suspend fun getMachines(serverId: String): Result<List<Machine>>
    suspend fun createMachine(serverId: String, name: String): Result<CreateMachineResponse>
    suspend fun renameMachine(serverId: String, machineId: String, name: String): Result<Machine>
    suspend fun deleteMachine(serverId: String, machineId: String): Result<Unit>
}

class MachineRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder
) : MachineRepository {

    override suspend fun getMachines(serverId: String): Result<List<Machine>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getMachines()
            Log.d("MachineRepo", "getMachines API: code=${response.code()}, bodySize=${response.body()?.size}")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = try { response.errorBody()?.string()?.take(200) } catch (_: Exception) { null }
                Log.e("MachineRepo", "getMachines failed: code=${response.code()}, error=$errorMsg")
                Result.failure(Exception("Get machines failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("MachineRepo", "getMachines exception", e)
            Result.failure(e)
        }
    }

    override suspend fun createMachine(serverId: String, name: String): Result<CreateMachineResponse> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.createMachine(CreateMachineRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Create machine failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameMachine(serverId: String, machineId: String, name: String): Result<Machine> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.renameMachine(machineId, RenameMachineRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Rename machine failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMachine(serverId: String, machineId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.deleteMachine(machineId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete machine failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
