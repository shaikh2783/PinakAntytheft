package com.app.antitheft.data.repository

import com.app.antitheft.data.remote.ApiService
import com.app.antitheft.data.remote.LoginRequest

class LoginRepository(private val api: ApiService) {

    suspend fun login(email: String, password: String, deviceId: String) =
        api.login(
            LoginRequest(
                email = email,
                password = password,
                device_id = deviceId
            )
        )
}