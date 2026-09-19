package com.aman.featureapp.responses

data class Response<T> (
    val success : Boolean,
    val message : String?,
    val data : T ?
)