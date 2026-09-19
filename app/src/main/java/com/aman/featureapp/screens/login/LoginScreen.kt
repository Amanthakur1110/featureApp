package com.aman.featureapp.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.featureapp.R

@Composable
@Preview
fun LoginScreen(
    viewmodel: LoginViewModel = viewModel(),
    onNavigateToVerification: (() -> Unit)? = null
) {
    // If the step is CODE_INPUT, transition seamlessly to LoginScreen2
    if (viewmodel.currentStep == LoginStep.CODE_INPUT) {
        LoginScreen2(viewmodel = viewmodel)
        return
    }

    val focusManager = LocalFocusManager.current

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(R.drawable.gradient_back),
                contentDescription = "background_image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding() + 20.dp)
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.login_person),
                    contentDescription = "login_person",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(45.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Login", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(30.dp))

                val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                OutlinedTextField(
                    value = viewmodel.email,
                    enabled = !viewmodel.isWaiting,
                    onValueChange = { newValue ->
                        viewmodel.email = newValue
                        viewmodel.errorMessage = null
                    },
                    textStyle = TextStyle(fontSize = 18.sp),
                    label = { Text("Email Address", color = Color.Black) },
                    placeholder = { Text("example@gmail.com") },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.email),
                            contentDescription = "Email Image",
                            modifier = Modifier.size(30.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewmodel.login {
                                onNavigateToVerification?.invoke()
                            }
                        }
                    )
                )

                if (viewmodel.errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = viewmodel.errorMessage ?: "",
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    enabled = !viewmodel.isWaiting,
                    onClick = {
                        focusManager.clearFocus()
                        viewmodel.login {
                            onNavigateToVerification?.invoke()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    if (viewmodel.isWaiting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Sending Code...")
                    } else {
                        Text("Continue")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Terms & Conditions notice ─────────────────────────────
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF9E9E9E), fontSize = 11.sp)) {
                            append("By continuing, you agree to our ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF424242),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append("Terms of Service")
                        }
                        withStyle(SpanStyle(color = Color(0xFF9E9E9E), fontSize = 11.sp)) {
                            append(" and ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF424242),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append("Privacy Policy")
                        }
                        withStyle(SpanStyle(color = Color(0xFF9E9E9E), fontSize = 11.sp)) {
                            append(".")
                        }
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // ──────────────────────────────────────────────────────────

                Spacer(modifier = Modifier.height(20.dp))

                // ── Info Card (bottom) ─────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F3F3),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.info_logo),
                            contentDescription = "Info",
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 1.dp),
                            colorFilter = ColorFilter.tint(Color(0xFF9E9E9E))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "We'll send a verification code to this email.",
                                fontSize = 13.sp,
                                color = Color(0xFF616161),
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your email is used as your primary identity. Make sure it's correct before continuing.",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                // ──────────────────────────────────────────────────────────
            }
        }
    }
}