package com.sri.aham.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sri.aham.home.HomeScreen
import com.sri.aham.mantra.ui.MantraScreen

/**
 * Top-level navigation route identifiers.
 * Using constants prevents typos and makes refactoring straightforward.
 */
object Route {
    const val HOME   = "home"
    const val MANTRA = "mantra"
    // Add future routes here (e.g. JOURNAL, BREATH_TIMER, POMODORO).
}

/**
 * Root Compose Navigation graph for the entire app.
 *
 * The [NavHost] starts at [Route.HOME]. Feature screens are added as [composable]
 * destinations and reached via [NavController.navigate] calls originating in
 * [HomeScreen] (or other screens in the future).
 *
 * To add a new feature:
 *  1. Add a constant to [Route].
 *  2. Add a `composable(Route.YOUR_FEATURE) { YourScreen() }` block below.
 *  3. Add a card to [HomeScreen] that calls `onNavigate(Route.YOUR_FEATURE)`.
 */
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.HOME,
    ) {
        composable(Route.HOME) {
            HomeScreen(onNavigate = navController::navigate)
        }
        composable(Route.MANTRA) {
            MantraScreen()
        }
    }
}
