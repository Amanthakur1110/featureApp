package com.aman.featureapp.screens.splash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class SplashViewModel: ViewModel() {
    val message : String  by mutableStateOf("server is down please try after some time")
}