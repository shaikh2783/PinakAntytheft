package com.app.antitheft.data.response

import com.google.gson.annotations.SerializedName

data class LoginResponseDataModel(
    val status: Boolean?,
    val code: Int?,
    val message: String?,
    val data: LoginResponseModel?
)

data class LoginResponseModel(
    val id: Int?,
    val first_name: String?,
    val last_name: String?,
    val email: String?,
    val mobile: String?,
    val status: String?,
    val role: String?,
    val device_id: String?,
    val subscription_activated: Int?,
    val is_register: Int?,
    val is_subscribed: Boolean?,
    val token: String?,
    val subscription_info: SubscriptionInfo?
)
data class SubscriptionInfo(
    val id: Int?,
    val dealer_id: Int?,
    val shop_id: Int?,
    val user_id: Int?,
    val code: String?,
    val subscription_id: Int?,
    val device_info: String?,
    val status: String?,
    val generate_at: String?,
    val expiry_at: String?,
    val activated_at: String?,
    val created_at: String?,
    val updated_at: String?,
    val subscription: SubscriptionDetails?
)
data class SubscriptionDetails(
    val id: Int?,
    val title: String?,
    val description: String?,
    val days: Int?,
    val price: String?,
    val status: String?,
    val created_at: String?,
    val updated_at: String?
)
