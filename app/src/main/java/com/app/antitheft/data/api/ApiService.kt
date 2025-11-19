package com.app.antitheft.data.remote

import com.app.antitheft.data.response.LoginResponseDataModel
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("/api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponseDataModel

    @Multipart
    @POST("api/update/capture_picture")
    suspend fun uploadCapturePicture(
        @Part("user_id") userId: RequestBody,
        @Part("type") type: RequestBody,
        @Part frontImage: MultipartBody.Part,
        @Part backImage: MultipartBody.Part
    ): Response<ResponseBody>
}
