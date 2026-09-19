package com.aman.featureapp.api

import com.aman.featureapp.constant.AppConstant
import com.aman.featureapp.session.UserSession
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiServer {
    private val BaseUrl = AppConstant.Network.BaseUrl

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = UserSession.getToken()

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        chain.proceed(newRequest)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: authApis = retrofit.create(authApis::class.java)
    val featureApi: featureApis = retrofit.create(featureApis::class.java)
}