package com.app.antitheft.data.remote

import com.app.antitheft.data.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse
}
