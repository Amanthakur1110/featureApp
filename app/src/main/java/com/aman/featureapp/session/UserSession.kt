package com.aman.featureapp.session

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object UserSession {

    private var tokenManager: TokenManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedToken: String? = null

    private val _email = MutableStateFlow<String?>(null)
    val email: StateFlow<String?> = _email.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun init(context: Context) {
        init(TokenManager(context))
    }

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
        runCatching {
            runBlocking {
                val (token, savedEmail) = tokenManager.getInitialSession()
                cachedToken = token
                _email.value = savedEmail
                _isLoggedIn.value = !token.isNullOrBlank()
            }
        }.onFailure {
            cachedToken = null
            _email.value = null
            _isLoggedIn.value = false
        }

        // Keep in-memory cache updated with any DataStore emissions
        scope.launch {
            tokenManager.tokenFlow.collect { token ->
                cachedToken = token
                _isLoggedIn.value = !token.isNullOrBlank()
            }
        }
        scope.launch {
            tokenManager.emailFlow.collect { savedEmail ->
                _email.value = savedEmail
            }
        }
    }

    fun saveSession(token: String, email: String) {
        cachedToken = token
        _email.value = email
        _isLoggedIn.value = true
        scope.launch {
            tokenManager?.saveSession(token, email)
        }
    }

    fun saveToken(token: String, email: String? = null) {
        if (email != null) {
            saveSession(token, email)
        } else {
            cachedToken = token
            _isLoggedIn.value = true
            scope.launch {
                tokenManager?.saveToken(token)
            }
        }
    }

    fun getToken(): String? {
        return cachedToken
    }

    fun getEmail(): String? {
        return _email.value
    }

    fun logout() {
        cachedToken = null
        _email.value = null
        _isLoggedIn.value = false
        scope.launch {
            tokenManager?.clearSession()
        }
    }

    fun clearToken() {
        logout()
    }
}
