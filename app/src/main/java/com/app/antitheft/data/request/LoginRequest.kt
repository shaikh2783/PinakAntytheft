package com.app.antitheft.data.remote

data class LoginRequest(
    val type: String="email",
    val email: String,
    val password: String,
    val device_id: String ,
)
