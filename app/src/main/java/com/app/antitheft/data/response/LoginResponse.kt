package com.app.antitheft.data.response

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserData? = null
)

data class UserData(
    val id: Int,
    val name: String,
    val email: String
)
