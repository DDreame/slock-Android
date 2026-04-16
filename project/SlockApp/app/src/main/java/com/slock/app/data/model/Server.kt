package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

data class Server(
    val id: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val role: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null
)

data class CreateServerRequest(
    val name: String,
    val slug: String
)

data class Member(
    val id: String? = null,
    @SerializedName("userId")
    val userId: String? = null,
    val role: String? = null,
    val user: User? = null,
    val name: String? = null,
    @SerializedName("displayName")
    val displayName: String? = null
)

data class UpdateMemberRoleRequest(
    val role: String
)

data class SidebarOrder(
    @SerializedName("channelOrder")
    val channelOrder: List<String> = emptyList(),
    @SerializedName("agentOrder")
    val agentOrder: List<String> = emptyList(),
    @SerializedName("dmOrder")
    val dmOrder: List<String> = emptyList()
)
