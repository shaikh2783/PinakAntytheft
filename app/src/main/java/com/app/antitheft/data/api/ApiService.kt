package com.app.antitheft.data.remote

import com.app.antitheft.data.response.LoginResponseDataModel
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("/api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponseDataModel

}
