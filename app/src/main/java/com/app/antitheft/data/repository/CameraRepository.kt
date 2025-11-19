package com.app.antitheft.data.repository

import com.app.antitheft.data.remote.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class CameraRepository(
    private val api: ApiService
) {

    suspend fun uploadFrontBack(userId: String, front: File, back: File): Boolean {
        // If you want compression <2MB, move your compressUnder2MB() here.

        val userPart = RequestBody.create("text/plain".toMediaType(), userId)
        val typePart = RequestBody.create("text/plain".toMediaType(), "camera")

        val frontBody = RequestBody.create("image/jpeg".toMediaType(), front)
        val backBody  = RequestBody.create("image/jpeg".toMediaType(), back)

        val frontPart = MultipartBody.Part.createFormData(
            "front_image",
            front.name,
            frontBody
        )
        val backPart = MultipartBody.Part.createFormData(
            "back_image",
            back.name,
            backBody
        )

        val resp = api.uploadCapturePicture(
            userId = userPart,
            type = typePart,
            frontImage = frontPart,
            backImage = backPart
        )

        return resp.isSuccessful
    }

    companion object {
        fun create(): CameraRepository {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://login.pinaksecurity.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)
            return CameraRepository(api)
        }
    }
}
