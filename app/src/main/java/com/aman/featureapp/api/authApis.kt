package com.aman.featureapp.api

import com.aman.featureapp.requests.loginViaEmail
import com.aman.featureapp.responses.Response
import com.aman.featureapp.responses.loginViaEmailResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface authApis {
    @POST("auth/login")
    suspend fun login(
        @Body request: loginViaEmail
    ): Response<loginViaEmailResponse>

    @POST("auth/verify-code")
    suspend fun verifyCode(
        @Body request: loginViaEmail
    ): Response<loginViaEmailResponse>
}