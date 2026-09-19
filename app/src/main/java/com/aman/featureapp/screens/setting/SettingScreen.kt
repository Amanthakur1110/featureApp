package com.aman.featureapp.screens.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.featureapp.R

@Composable
fun SettingScreen(
    viewModel: SettingViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val userEmail by viewModel.userEmail.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // Top Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1B1F),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Account Section with horizontal dividers: ------------ Account ------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = Color(0xFFE0E0E0)
            )
            Text(
                text = "Account",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF757575),
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = Color(0xFFE0E0E0)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Logout Button / Item in Account Section
        Card(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFBFBFB)
            ),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logout),
                    contentDescription = "Logout",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                if (!userEmail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "($userEmail)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E9E9E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}