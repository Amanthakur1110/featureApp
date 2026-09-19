package com.aman.featureapp.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.featureapp.R

@Composable
@Preview
fun LoginScreen2(
    viewmodel: LoginViewModel = viewModel(),
    onBackToEmail: (() -> Unit)? = null
) {
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
                    contentDescription = "verification_icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(45.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Verification Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter the 6-digit code sent to\n${viewmodel.email}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                OutlinedTextField(
                    value = viewmodel.code,
                    enabled = !viewmodel.isWaiting,
                    onValueChange = { newValue ->
                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                            viewmodel.code = newValue
                            viewmodel.errorMessage = null
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 6.sp
                    ),
                    label = { Text("6-Digit Code", color = Color.Black) },
                    placeholder = {
                        Text(
                            "• • • • • •",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
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
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewmodel.verifyCode()
                        }
                    )
                )

                if (viewmodel.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = viewmodel.errorMessage ?: "",
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (viewmodel.statusMessage != null && viewmodel.errorMessage == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewmodel.statusMessage ?: "",
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    enabled = !viewmodel.isWaiting && viewmodel.code.length == 6,
                    onClick = {
                        focusManager.clearFocus()
                        viewmodel.verifyCode()
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
                        Text("Verifying...")
                    } else {
                        Text("Verify & Continue")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewmodel.backToEmail()
                            onBackToEmail?.invoke()
                        },
                        enabled = !viewmodel.isWaiting
                    ) {
                        Text("Change Email", color = Color.Black, fontSize = 14.sp)
                    }

                    TextButton(
                        onClick = {
                            viewmodel.resendCode()
                        },
                        enabled = !viewmodel.isWaiting
                    ) {
                        Text("Resend Code", color = Color(0xFF1976D2), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}