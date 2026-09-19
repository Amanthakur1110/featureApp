package com.aman.featureapp.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class TokenManager(private val context: Context) {

    companion object {
        val KEY_TOKEN = stringPreferencesKey("auth_token")
        val KEY_EMAIL = stringPreferencesKey("user_email")
    }

    val tokenFlow: Flow<String?> = context.userDataStore.data.map { preferences ->
        preferences[KEY_TOKEN]
    }

    val emailFlow: Flow<String?> = context.userDataStore.data.map { preferences ->
        preferences[KEY_EMAIL]
    }

    suspend fun saveSession(token: String, email: String) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
            preferences[KEY_EMAIL] = email
        }
    }

    suspend fun saveToken(token: String) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
        }
    }

    suspend fun saveEmail(email: String) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_EMAIL] = email
        }
    }

    suspend fun clearSession() {
        context.userDataStore.edit { preferences ->
            preferences.remove(KEY_TOKEN)
            preferences.remove(KEY_EMAIL)
        }
    }

    suspend fun clearToken() {
        clearSession()
    }

    suspend fun getInitialSession(): Pair<String?, String?> {
        val preferences = context.userDataStore.data.first()
        return Pair(preferences[KEY_TOKEN], preferences[KEY_EMAIL])
    }
}
