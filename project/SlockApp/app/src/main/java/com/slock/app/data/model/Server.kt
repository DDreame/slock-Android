package com.slock.app.data.model

import com.google.gson.annotations.SerializedName

data class Server(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val role: String = "member",
    @SerializedName("createdAt")
    val createdAt: String = ""
)

data class CreateServerRequest(
    val name: String,
    val slug: String
)

data class Member(
    val id: String,
    @SerializedName("userId")
    val userId: String,
    val role: String,
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
