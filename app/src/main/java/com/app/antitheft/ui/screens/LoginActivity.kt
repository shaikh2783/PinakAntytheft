package com.app.antitheft.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import com.app.antitheft.Helper.datastore.DataStoreManager
import com.app.antitheft.MainActivity
import com.app.antitheft.ui.screens.LoginScreenContent
import com.app.antitheft.viewmodel.LoginViewModel

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initDataStore(DataStoreManager.getInstance(this))

        setContent {

            val state = viewModel.uiState.collectAsState().value

            LoginScreenContent(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onSubmit = {
                    viewModel.login(this) {
                        navigateToHome()
                    }
                }
            )
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

