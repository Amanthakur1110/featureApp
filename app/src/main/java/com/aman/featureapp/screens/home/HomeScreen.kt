package com.aman.featureapp.screens.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay as coroutineDelay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.featureapp.screens.webview.FeatureWebViewActivity

// ─── Design tokens ───────────────────────────────────────────────────────────
private val PageGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFF3C7B6), Color(0xFFF7E9E2), Color(0xFFFAF7F5))
)
private val BgCard    = Color.White
private val TextPrimary   = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF8B8580)
private val TextHint  = Color(0xFFA39C97)
private val AccentOrange = Color(0xFFE8734A)
private val AccentBlue  = Color(0xFF3B82F6)
private val AccentBlueDark = Color(0xFF2563EB)
private val AccentGreen = Color(0xFF10B981)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentRed   = Color(0xFFEF4444)

private val BubbleUserGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2A2A2A), Color(0xFF141414))
)

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabBarHeight = 54.dp + navBottom
    val imeLift = (imeBottom - tabBarHeight).coerceAtLeast(0.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = imeLift)
        ) {
            TopBar(onNewFeatureClicked = { viewModel.startNewFeature() })

            // Active sessions tab strip (only shown when more than 1 or status exists)
            val showTabs = state.sessions.size > 1 || state.sessions.any {
                it.latestStatus !is FeatureBuildStatus.Idle ||
                        it.uid != null ||
                        it.currentStep == ChatStep.EDITING_DESCRIPTION
            }
            if (showTabs) {
                SessionTabStrip(
                    sessions = state.sessions,
                    activeSessionId = state.activeSessionId,
                    onSelectSession = viewModel::selectSession,
                    onCloseSession = viewModel::closeSession,
                    onNewClicked = viewModel::startNewFeature
                )
            }

            // Slim status ribbon
            StatusRibbon(
                status = state.latestStatus,
                onOpenFeature = { uid, name ->
                    context.startActivity(
                        Intent(context, FeatureWebViewActivity::class.java).apply {
                            putExtra(FeatureWebViewActivity.EXTRA_FEATURE_UID, uid)
                            putExtra(FeatureWebViewActivity.EXTRA_FEATURE_NAME, name)
                            putExtra(FeatureWebViewActivity.EXTRA_IS_PUBLIC, true)
                        }
                    )
                },
                onRetry = { viewModel.startNewFeature() }
            )

            // Chat feed OR centered greeting
            if (state.chatMessages.isEmpty()) {
                GreetingCenter(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.chatMessages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }
                }
            }

            // Input bar anchored to bottom
            InputBar(
                text = state.inputText,
                onTextChanged = viewModel::onInputChanged,
                onSend = { keyboardController?.hide(); viewModel.onSendClicked() },
                step = state.currentStep,
                isSending = state.isSending,
                status = state.latestStatus
            )
        }
    }
}

// ─── Top Bar (menu · title · profile) ────────────────────────────────────────

@Composable
private fun TopBar(onNewFeatureClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,

            modifier = Modifier.clickable { }
        ) {
            Text("Feature App", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TextPrimary)
                .clickable { onNewFeatureClicked() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "New feature", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Centered greeting (empty state) ─────────────────────────────────────────

@Composable
private fun GreetingCenter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✳",
            fontSize = 40.sp,
            color = AccentOrange
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "How can I help you\nbuild today?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
    }
}

// ─── Status Ribbon ───────────────────────────────────────────────────────────

@Composable
private fun StatusRibbon(
    status: FeatureBuildStatus,
    onOpenFeature: (String, String) -> Unit,
    onRetry: () -> Unit
) {
    AnimatedVisibility(
        visible = status !is FeatureBuildStatus.Idle,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it }
    ) {
        when (status) {
            is FeatureBuildStatus.Building -> {
                val elapsed by produceState(
                    initialValue = ((System.currentTimeMillis() - status.startTimeMs) / 1000).coerceAtLeast(0),
                    key1 = status.startTimeMs
                ) {
                    while (true) {
                        coroutineDelay(1000L)
                        value = ((System.currentTimeMillis() - status.startTimeMs) / 1000).coerceAtLeast(0)
                    }
                }
                val t = if (elapsed >= 60) "${elapsed / 60}m ${elapsed % 60}s" else "${elapsed}s"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(15.dp), color = AccentBlue, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Creating \"${status.featureName}\"…", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D4ED8), modifier = Modifier.weight(1f))
                    Text(t, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentBlueDark)
                }
            }
            is FeatureBuildStatus.Clarifying -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Needs your input — answer below", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF92400E))
                }
            }
            is FeatureBuildStatus.Ready -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("\"${status.featureName}\" is ready!", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF065F46), modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onOpenFeature(status.uid, status.featureName) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Launch, null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            is FeatureBuildStatus.Failed -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Something went wrong — try again", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF991B1B), modifier = Modifier.weight(1f))
                    IconButton(onClick = onRetry, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
            is FeatureBuildStatus.Idle -> Unit
        }
    }
}

// ─── Chat Bubble ─────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(message: UiChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser) Color.Transparent else BgCard,
            shadowElevation = if (isUser) 0.dp else 1.5.dp,
            modifier = Modifier
                .widthIn(min = 40.dp, max = 300.dp)
                .then(
                    if (isUser) Modifier.background(
                        BubbleUserGradient,
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
                    ) else Modifier
                )
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = if (isUser) Color.White else TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
            )
        }
    }
}

// ─── Input Bar (pill style, matches reference) ───────────────────────────────

@Composable
private fun InputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    step: ChatStep,
    isSending: Boolean,
    status: FeatureBuildStatus
) {
    val isBuilding = status is FeatureBuildStatus.Building
    val enabled = !isBuilding && !isSending || step == ChatStep.EDITING_DESCRIPTION && !isSending
    val placeholder = when {
        step == ChatStep.ASKING_NAME -> "Give your app a name…"
        step == ChatStep.ASKING_DESCRIPTION -> "Describe what it should do…"
        step == ChatStep.EDITING_DESCRIPTION -> "What changes do you want?"
        status is FeatureBuildStatus.Clarifying -> "Type your answer…"
        isBuilding -> "Building your app…"
        else -> "Chat with App Creator"
    }

    val btnActive = enabled && text.isNotBlank()
    val btnBg by animateColorAsState(
        targetValue = if (btnActive) TextPrimary else Color(0xFFD8D2CC),
        animationSpec = tween(150),
        label = "sendBtnBg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            color = BgCard,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Text field row
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = { Text(placeholder, fontSize = 14.sp, color = TextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = TextPrimary
                    ),
                    maxLines = 5,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

                // Icon row: spacer · send
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 10.dp, bottom = 8.dp, top = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(btnBg)
                            .clickable(
                                enabled = btnActive,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, "Send",
                                tint = if (btnActive) Color.White else Color(0xFF9C9690),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Session Tab Strip ───────────────────────────────────────────────────────

@Composable
private fun SessionTabStrip(
    sessions: List<ActiveFeatureSession>,
    activeSessionId: String,
    onSelectSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onNewClicked: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(sessions, key = { it.id }) { session ->
                val selected = session.id == activeSessionId
                val statusColor = when (session.latestStatus) {
                    is FeatureBuildStatus.Building -> AccentBlue
                    is FeatureBuildStatus.Clarifying -> AccentAmber
                    is FeatureBuildStatus.Ready -> AccentGreen
                    is FeatureBuildStatus.Failed -> AccentRed
                    is FeatureBuildStatus.Idle -> TextSecondary
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) TextPrimary else Color.White.copy(alpha = 0.65f),
                    shadowElevation = if (selected) 3.dp else 0.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectSession(session.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (selected) Color.White.copy(alpha = 0.7f) else statusColor)
                        )
                        Column {
                            Text(
                                text = session.featureName.ifBlank { "New App" },
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = when (session.latestStatus) {
                                    is FeatureBuildStatus.Building -> "Creating…"
                                    is FeatureBuildStatus.Clarifying -> "Needs answer"
                                    is FeatureBuildStatus.Ready -> "Ready"
                                    is FeatureBuildStatus.Failed -> "Failed"
                                    is FeatureBuildStatus.Idle -> if (session.currentStep == ChatStep.EDITING_DESCRIPTION) "Editing"
                                    else if (session.currentStep == ChatStep.ASKING_DESCRIPTION) "Drafting" else "New"
                                },
                                fontSize = 10.sp,
                                color = if (selected) Color.White.copy(alpha = 0.55f) else TextSecondary
                            )
                        }
                        if (sessions.size > 1 || session.latestStatus !is FeatureBuildStatus.Idle) {
                            IconButton(onClick = { onCloseSession(session.id) }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, null, tint = if (selected) Color.White.copy(alpha = 0.6f) else TextSecondary, modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onNewClicked() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = TextPrimary, modifier = Modifier.size(13.dp))
                        Text("New", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }
    }
}