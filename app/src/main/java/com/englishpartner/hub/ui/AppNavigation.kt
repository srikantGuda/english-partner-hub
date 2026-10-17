package com.englishpartner.hub.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.englishpartner.hub.ui.common.appContainer
import com.englishpartner.hub.ui.day.DayScreen
import com.englishpartner.hub.ui.home.HomeScreen
import com.englishpartner.hub.ui.library.LibraryScreen
import com.englishpartner.hub.ui.settings.SettingsScreen
import com.englishpartner.hub.ui.share.ShareImportScreen
import com.englishpartner.hub.ui.viewer.MaterialScreen
import com.englishpartner.hub.ui.vocab.VocabularyScreen

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val VOCAB = "vocab"
    const val SETTINGS = "settings"
    const val DAY = "day/{dayId}"
    const val MATERIAL = "material/{materialId}"
    const val SHARE = "share"

    fun day(id: Long) = "day/$id"
    fun material(id: Long) = "material/$id"
}

private data class TopTab(val route: String, val label: String, val icon: ImageVector)

private val topTabs = listOf(
    TopTab(Routes.HOME, "Home", Icons.Filled.Home),
    TopTab(Routes.LIBRARY, "Library", Icons.Filled.School),
    TopTab(Routes.VOCAB, "Words", Icons.Filled.Translate),
    TopTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = topTabs.any { it.route == currentRoute }

    // When something is shared into the app, open the "Add to course" screen.
    val inbox = appContainer().shareInbox
    val pending by inbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        if (pending != null) {
            nav.navigate(Routes.SHARE) {
                popUpTo(Routes.SHARE) { inclusive = true }
                launchSingleTop = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { outer ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier
                .padding(bottom = outer.calculateBottomPadding())
                // Tell inner screens the bottom inset is already handled, so it is not added twice.
                .consumeWindowInsets(PaddingValues(bottom = outer.calculateBottomPadding()))
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenMaterial = { nav.navigate(Routes.material(it)) },
                    onOpenDay = { nav.navigate(Routes.day(it)) },
                    onOpenLibrary = { nav.navigateToTab(Routes.LIBRARY) }
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(onOpenDay = { nav.navigate(Routes.day(it)) })
            }
            composable(Routes.VOCAB) { VocabularyScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(
                Routes.DAY,
                arguments = listOf(navArgument("dayId") { type = NavType.LongType })
            ) { entry ->
                val dayId = entry.arguments?.getLong("dayId") ?: 0L
                DayScreen(
                    dayId = dayId,
                    onBack = { nav.popBackStack() },
                    onOpenMaterial = { nav.navigate(Routes.material(it)) }
                )
            }
            composable(
                Routes.MATERIAL,
                arguments = listOf(navArgument("materialId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("materialId") ?: 0L
                MaterialScreen(materialId = id, onBack = { nav.popBackStack() })
            }
            composable(Routes.SHARE) {
                ShareImportScreen(
                    onImported = { dayId ->
                        nav.navigate(Routes.day(dayId)) {
                            popUpTo(Routes.SHARE) { inclusive = true }
                        }
                    },
                    onCancel = {
                        if (!nav.popBackStack()) nav.navigate(Routes.HOME)
                    }
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
