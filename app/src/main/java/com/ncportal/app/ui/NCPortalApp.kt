package com.ncportal.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.ncportal.app.ui.explorer.ExplorerScreen
import com.ncportal.app.ui.home.HomeScreen
import com.ncportal.app.ui.settings.SettingsScreen
import com.ncportal.app.ui.settings.ThemeMode
import com.ncportal.app.ui.theme.NCPortalTheme
import com.ncportal.app.ui.work.WorkScreen

/** The portal's top-level sections, shown as bottom navigation destinations. */
enum class PortalTab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Filled.Home),
    FILES("파일", Icons.Filled.Folder),
    WORK("업무", Icons.Filled.Work),
    SETTINGS("설정", Icons.Filled.Settings),
}

/**
 * App root: owns theme preferences (kept across config changes via rememberSaveable)
 * and the bottom-tab shell. Each tab renders its own screen; tab switching is plain
 * state (no navigation library) to keep the dependency set minimal.
 */
@Composable
fun NCPortalApp() {
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    var dynamicColor by rememberSaveable { mutableStateOf(true) }

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    NCPortalTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        var selectedTab by rememberSaveable { mutableStateOf(PortalTab.HOME) }

        Scaffold(
            // Each screen handles its own top (status bar) inset via a top bar;
            // the NavigationBar handles the bottom system inset itself.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    PortalTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (selectedTab) {
                    PortalTab.HOME -> HomeScreen(onNavigateToFiles = { selectedTab = PortalTab.FILES })
                    PortalTab.FILES -> ExplorerScreen()
                    PortalTab.WORK -> WorkScreen()
                    PortalTab.SETTINGS -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it },
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = { dynamicColor = it },
                    )
                }
            }
        }
    }
}
