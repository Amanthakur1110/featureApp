package com.aman.featureapp.screens.main

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.featureapp.R
import com.aman.featureapp.screens.feature.FeatureScreen
import com.aman.featureapp.screens.home.HomeScreen
import com.aman.featureapp.screens.home.HomeViewModel
import com.aman.featureapp.screens.setting.SettingScreen
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

data class MainTabItem(
    val title: String,
    @get:DrawableRes val selectedIcon: Int,
    @get:DrawableRes val unselectedIcon: Int
)

@Composable
fun MainScreen() {
    val tabs = listOf(
        MainTabItem(
            title = "Home",
            selectedIcon = R.drawable.home_fill,
            unselectedIcon = R.drawable.home_outline
        ),
        MainTabItem(
            title = "Feature",
            selectedIcon = R.drawable.feature_fill,
            unselectedIcon = R.drawable.feature_outline
        ),
        MainTabItem(
            title = "Setting",
            selectedIcon = R.drawable.setting_fill,
            unselectedIcon = R.drawable.setting_outline
        )
    )

    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // Hoist HomeViewModel here so FeatureScreen can trigger edit flow on it
    val homeViewModel: HomeViewModel = viewModel()

    // Back click handler:
    // If not on Home page (index 0), navigate back to Home page.
    // If already on Home page, BackHandler is disabled, allowing default system back (exit/minimize).
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.scrollToPage(0)
        }
    }

    Scaffold(
        bottomBar = {
            CustomYouTubeTabBar(
                tabs = tabs,
                currentPage = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.scrollToPage(index)
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) { page ->
            when (page) {
                0 -> HomeScreen(viewModel = homeViewModel)
                1 -> FeatureScreen(
                    onEditFeature = { uid, name ->
                        homeViewModel.startEditFeature(uid, name)
                        coroutineScope.launch { pagerState.scrollToPage(0) }
                    },
                    onFeatureDeleted = { deletedUid ->
                        homeViewModel.onFeatureDeleted(deletedUid)
                    }
                )
                2 -> SettingScreen()
            }
        }
    }
}

/**
 * Custom modern bottom tab bar:
 * - Solid pure white background (#FFFFFF)
 * - Crisp hairline top divider (#E2E8F0)
 * - Clean icon + 11sp label layout without pill container highlight
 * - Active: #0F172A (dark slate) with filled icon
 * - Inactive: #64748B (slate gray) with outline icon
 * - Handles navigation bars insets properly
 */
@Composable
fun CustomYouTubeTabBar(
    tabs: List<MainTabItem>,
    currentPage: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(Color(0xFFE2E8F0))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = currentPage == index
                    val itemColor = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 28.dp)
                            ) {
                                onTabSelected(index)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isSelected) tab.selectedIcon else tab.unselectedIcon
                            ),
                            contentDescription = tab.title,
                            tint = itemColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = itemColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}