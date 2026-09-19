package com.aman.featureapp.screens.setting

import androidx.lifecycle.ViewModel
import com.aman.featureapp.session.UserSession
import kotlinx.coroutines.flow.StateFlow

class SettingViewModel : ViewModel() {

    val userEmail: StateFlow<String?> = UserSession.email

    fun logout() {
        UserSession.logout()
    }
}
