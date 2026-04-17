package com.slock.app.data.local.dao

import androidx.room.*
import com.slock.app.data.local.entity.AgentEntity

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents WHERE serverId = :serverId ORDER BY createdAt DESC")
    suspend fun getAgentsByServer(serverId: String): List<AgentEntity>

    @Query("SELECT * FROM agents WHERE id = :agentId")
    suspend fun getAgentById(agentId: String): AgentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgents(agents: List<AgentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)

    @Query("DELETE FROM agents WHERE id = :agentId")
    suspend fun deleteAgentById(agentId: String)

    @Query("DELETE FROM agents WHERE serverId = :serverId")
    suspend fun deleteAgentsByServer(serverId: String)

    @Query("SELECT * FROM agents WHERE serverId = :serverId AND name LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun searchAgents(serverId: String, query: String, limit: Int = 20): List<AgentEntity>
}
