package com.videonote.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProviderDto(
    val id: String,
    val name: String,
    val logo: String,
    val type: String,
    val api_key: String,
    val base_url: String,
    val enabled: Int,
    val created_at: String
)

@Serializable
data class ProviderRequest(
    val name: String,
    val api_key: String,
    val base_url: String,
    val logo: String? = null,
    val type: String
)

@Serializable
data class ProviderUpdateRequest(
    val id: String,
    val name: String? = null,
    val api_key: String? = null,
    val base_url: String? = null,
    val logo: String? = null,
    val type: String? = null,
    val enabled: Int? = null
)

@Serializable
data class TestRequest(
    val id: String
)