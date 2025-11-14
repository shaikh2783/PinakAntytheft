package com.app.antitheft.Helper.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class DataStoreManager private constructor(private val context: Context) {

    private val Context.dataStore by preferencesDataStore("user_prefs")

    companion object {
        @Volatile
        private var INSTANCE: DataStoreManager? = null

        fun getInstance(context: Context): DataStoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataStoreManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    suspend fun saveLoginData(
        token: String,
        userId: String,
        email: String?,
        name: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN] = token
            prefs[USER_ID] = userId
            email?.let { prefs[USER_EMAIL] = it }
            name?.let { prefs[USER_NAME] = it }
        }
    }

    val getToken: Flow<String?> = context.dataStore.data.map { it[TOKEN] }
    val getUserId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
}

