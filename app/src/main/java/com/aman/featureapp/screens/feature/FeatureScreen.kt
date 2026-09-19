package com.aman.featureapp.screens.feature

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.featureapp.responses.feature.FeatureItem
import com.aman.featureapp.screens.webview.FeatureWebViewActivity

private val BgPage     = Color(0xFFF7F8FA)
private val BgCard     = Color.White
private val BgInput    = Color(0xFFF1F5F9)
private val Divider    = Color(0xFFE8ECF0)
private val TextPrimary   = Color(0xFF0D1117)
private val TextSecondary = Color(0xFF64748B)
private val TextHint      = Color(0xFFADB5BD)

@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = viewModel(),
    onEditFeature: (uid: String, name: String) -> Unit = { _, _ -> },
    onFeatureDeleted: (uid: String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var featureToDelete by remember { mutableStateOf<FeatureItem?>(null) }

    LaunchedEffect(Unit) { viewModel.loadFeatures() }

    val filteredFeatures = state.features.filter { feat ->
        val matchesSearch = state.searchQuery.isBlank() ||
                feat.name.contains(state.searchQuery, ignoreCase = true) ||
                feat.description.contains(state.searchQuery, ignoreCase = true)
        val matchesFilter = when (state.filterType) {
            FeatureFilter.ALL -> true
            FeatureFilter.FAVOURITES -> feat.is_favourite
            FeatureFilter.PUBLIC -> feat.is_public
        }
        matchesSearch && matchesFilter
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BgPage) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Header ─────────────────────────────────────────────────
            Surface(color = BgCard, shadowElevation = 0.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("My Apps", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text(
                                text = if (state.features.isEmpty()) "No apps yet"
                                       else "${state.features.size} app${if (state.features.size == 1) "" else "s"} created",
                                fontSize = 12.sp, color = TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BgInput)
                                .clickable { viewModel.loadFeatures() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // ── Search ──────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgInput)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, "Search", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Normal),
                            decorationBox = { inner ->
                                if (state.searchQuery.isEmpty()) Text("Search your apps…", fontSize = 14.sp, color = TextHint)
                                inner()
                            }
                        )
                        AnimatedVisibility(visible = state.searchQuery.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, "Clear", tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Filter chips ────────────────────────────────────
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            FeatureFilter.ALL to "All",
                            FeatureFilter.FAVOURITES to "Favourites",
                            FeatureFilter.PUBLIC to "Public"
                        ).forEach { (filter, label) ->
                            item {
                                FilterChip(
                                    selected = state.filterType == filter,
                                    onClick = { viewModel.onFilterSelected(filter) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TextPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                }
            }

            // ── List ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.features.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TextPrimary)
                        }
                    }
                    filteredFeatures.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🗂️", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No apps here",
                                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Go to Home and describe what you want to build!",
                                fontSize = 13.sp, color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredFeatures, key = { it.uid }) { feature ->
                                AppCard(
                                    feature = feature,
                                    onOpen = {
                                        context.startActivity(
                                            Intent(context, FeatureWebViewActivity::class.java).apply {
                                                putExtra(FeatureWebViewActivity.EXTRA_FEATURE_UID, feature.uid)
                                                putExtra(FeatureWebViewActivity.EXTRA_FEATURE_NAME, feature.name)
                                                putExtra(FeatureWebViewActivity.EXTRA_IS_PUBLIC, feature.is_public)
                                            }
                                        )
                                    },
                                    onToggleFav = { viewModel.toggleFavourite(feature) },
                                    onTogglePublic = { viewModel.togglePublic(feature) },
                                    onDelete = { featureToDelete = feature },
                                    onEdit = { onEditFeature(feature.uid, feature.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete dialog
    featureToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { featureToDelete = null },
            title = { Text("Delete \"${target.name}\"?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove the app and all its data.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFeature(target)
                        onFeatureDeleted(target.uid)
                        featureToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { featureToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── App Card ────────────────────────────────────────────────────────────────

@Composable
private fun AppCard(
    feature: FeatureItem,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit,
    onTogglePublic: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val gradients = listOf(
        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
        listOf(Color(0xFF3B82F6), Color(0xFF0EA5E9)),
        listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
    )
    val brush = Brush.linearGradient(gradients[kotlin.math.abs(feature.name.hashCode()) % gradients.size])

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BgCard,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gradient avatar
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(feature.name.take(1).uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            feature.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(status = feature.status)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        feature.description, fontSize = 12.sp, color = TextSecondary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionBtn(
                        onClick = onToggleFav,
                        icon = { Icon(
                            if (feature.is_favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null,
                            tint = if (feature.is_favourite) Color(0xFFE11D48) else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )}
                    )
                    ActionBtn(
                        onClick = onTogglePublic,
                        icon = { Icon(
                            if (feature.is_public) Icons.Default.Language else Icons.Default.Lock,
                            null,
                            tint = if (feature.is_public) Color(0xFF3B82F6) else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )}
                    )
                    ActionBtn(
                        onClick = onEdit,
                        icon = { Icon(Icons.Default.Edit, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp)) }
                    )
                    ActionBtn(
                        onClick = onDelete,
                        icon = { Icon(Icons.Default.Delete, null, tint = TextHint, modifier = Modifier.size(18.dp)) }
                    )
                }

                if (feature.status == "ready") {
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Launch, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionBtn(onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { icon() }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg, label) = when (status) {
        "ready"      -> Triple(Color(0xFFECFDF5), Color(0xFF059669), "Ready")
        "building"   -> Triple(Color(0xFFEFF6FF), Color(0xFF3B82F6), "Building")
        "clarifying" -> Triple(Color(0xFFFFFBEB), Color(0xFFD97706), "Question")
        "error"      -> Triple(Color(0xFFFFF1F2), Color(0xFFEF4444), "Error")
        else         -> Triple(Color(0xFFF1F5F9), TextSecondary, status)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}