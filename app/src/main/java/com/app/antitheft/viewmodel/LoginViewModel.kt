package com.app.antitheft.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.app.antitheft.Helper.datastore.DataStoreManager
import com.app.antitheft.data.repository.LoginRepository
import com.app.antitheft.data.network.RetrofitClient

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel : ViewModel() {

    private val repo = LoginRepository(RetrofitClient.api)
    private lateinit var dataStore: DataStoreManager

    fun initDataStore(ds: DataStoreManager) {
        dataStore = ds
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()
    fun onEmailChange(v: String) =
        _uiState.update { it.copy(email = v) }

    fun onPasswordChange(v: String) =
        _uiState.update { it.copy(password = v) }


    fun login(context: Context, onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email & Password required") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val deviceId = DeviceUtil.getDeviceId(context)

                val response = repo.login(state.email, state.password, deviceId)

                if (response.status == true) {

                    dataStore.saveLoginData(
                        token = response.data?.token ?: "",
                        userId = response.data?.id?.toString() ?: "",
                        email = response.data?.email ?: "",
                        name = response.data?.first_name ?: ""
                    )

                    onSuccess()

                } else {
                    _uiState.update { it.copy(errorMessage = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Unable to reach server") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

}

class DeviceUtil {

    companion object {
        fun getDeviceId(context: Context): String {
            return Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }
    }
}