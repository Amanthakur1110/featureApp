package com.aman.featureapp

import android.app.Application
import com.aman.featureapp.session.TokenManager
import com.aman.featureapp.session.UserSession

class FeatureApp : Application() {

    lateinit var tokenManager: TokenManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        tokenManager = TokenManager(applicationContext)
        UserSession.init(tokenManager)
    }

    companion object {
        lateinit var instance: FeatureApp
            private set
    }
}
