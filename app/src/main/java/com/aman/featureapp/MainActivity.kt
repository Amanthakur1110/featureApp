package com.aman.featureapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.featureapp.screens.login.LoginScreen
import com.aman.featureapp.screens.login.LoginViewModel
import com.aman.featureapp.screens.main.MainScreen
import com.aman.featureapp.screens.splash.SplashScreen
import com.aman.featureapp.session.UserSession
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            enterToApp()
        }
    }
}

@Composable
fun enterToApp() {
    var showSplash by remember { mutableStateOf(true) }
    val isLoggedIn by UserSession.isLoggedIn.collectAsState()

    // Shared LoginViewModel scoped at app level so both screens keep state across navigation
    val loginViewModel: LoginViewModel = viewModel()

    LaunchedEffect(Unit) {
        delay(3000L)
        showSplash = false
    }

    when {
        showSplash -> SplashScreen()
        isLoggedIn -> MainScreen()
        else -> {
            // LoginScreen internally switches to LoginScreen2 when step == CODE_INPUT.
            // Once verifyCode() succeeds, UserSession.saveToken() sets isLoggedIn = true,
            // which automatically recomposes this function and navigates to MainScreen.
            LoginScreen(viewmodel = loginViewModel)
        }
    }
}