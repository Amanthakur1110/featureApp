package com.aman.featureapp.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.featureapp.api.ApiServer
import com.aman.featureapp.api.parseServerError
import com.aman.featureapp.requests.loginViaEmail
import com.aman.featureapp.session.UserSession
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

enum class LoginStep {
    EMAIL_INPUT,
    CODE_INPUT
}

class LoginViewModel : ViewModel() {

    var currentStep: LoginStep by mutableStateOf(LoginStep.EMAIL_INPUT)
    var email: String by mutableStateOf("")
    var tempToken: String? by mutableStateOf(null)
    var code: String by mutableStateOf("")

    var isWaiting: Boolean by mutableStateOf(false)
    var errorMessage: String? by mutableStateOf(null)
    var statusMessage: String? by mutableStateOf(null)

    fun login(onSuccess: (() -> Unit)? = null) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            errorMessage = "Please enter a valid email address"
            return
        }

        isWaiting = true
        errorMessage = null
        statusMessage = null

        viewModelScope.launch {
            try {
                val request = loginViaEmail(
                    email = trimmedEmail,
                    token = null,
                    code = null
                )
                val response = ApiServer.authApi.login(request)
                if (response.success && response.data != null) {
                    tempToken = response.data.token
                    statusMessage = response.data.message ?: response.message ?: "Verification code sent"
                    currentStep = LoginStep.CODE_INPUT
                    onSuccess?.invoke()
                } else {
                    errorMessage = response.message ?: "Failed to send verification code"
                }
            } catch (e: HttpException) {
                // Server returned a non-2xx response — extract the server's message field
                errorMessage = parseServerError(e, fallback = "Request failed (${e.code()})")
            } catch (e: IOException) {
                errorMessage = "No connection. Please check your internet or server."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Unexpected error. Please try again."
            } finally {
                isWaiting = false
            }
        }
    }

    fun verifyCode(onSuccess: (() -> Unit)? = null) {
        val trimmedCode = code.trim()
        if (trimmedCode.length != 6) {
            errorMessage = "Please enter the 6-digit verification code"
            return
        }

        isWaiting = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val request = loginViaEmail(
                    email = email.trim(),
                    token = tempToken,
                    code = trimmedCode
                )
                val response = ApiServer.authApi.verifyCode(request)
                if (response.success && response.data?.token != null) {
                    val jwtAuthToken = response.data.token
                    val userEmail = response.data.email ?: email.trim()
                    UserSession.saveSession(token = jwtAuthToken, email = userEmail)
                    currentStep = LoginStep.EMAIL_INPUT
                    email = ""
                    code = ""
                    tempToken = null
                    onSuccess?.invoke()
                } else {
                    errorMessage = response.message ?: "Invalid or expired verification code"
                }
            } catch (e: HttpException) {
                // Server returned a non-2xx response — extract the server's message field
                errorMessage = parseServerError(e, fallback = "Verification failed (${e.code()})")
            } catch (e: IOException) {
                errorMessage = "No connection. Please check your internet or server."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Unexpected error. Please try again."
            } finally {
                isWaiting = false
            }
        }
    }

    fun resendCode() {
        code = ""
        login()
    }

    fun backToEmail() {
        code = ""
        errorMessage = null
        statusMessage = null
        currentStep = LoginStep.EMAIL_INPUT
    }
}