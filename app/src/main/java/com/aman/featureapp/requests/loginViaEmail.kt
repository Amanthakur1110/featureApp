package com.aman.featureapp.requests

data class loginViaEmail(
    val email: String,
    val token: String?,
    val code : String?
)
