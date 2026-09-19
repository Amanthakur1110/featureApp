package com.aman.featureapp.constant

object AppConstant {

    object Network {
        const val Host = "http://10.42.0.1:8004"
        const val BaseUrl = "$Host/api/v1/"

        fun getFeatureHtmlUrl(uid: String, token: String? = null): String {
            val cleanUid = uid.trim().trimEnd('/')
            return if (!token.isNullOrBlank()) {
                "$Host/feature/get/$cleanUid/?token=$token"
            } else {
                "$Host/feature/get/$cleanUid/"
            }
        }
    }
}