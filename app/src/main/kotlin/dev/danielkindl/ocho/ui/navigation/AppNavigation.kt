package dev.danielkindl.ocho.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.danielkindl.ocho.ui.home.HomeScreen
import dev.danielkindl.ocho.ui.licenses.LicensesScreen
import dev.danielkindl.ocho.ui.session.ActiveSessionScreen
import dev.danielkindl.ocho.ui.settings.SettingsScreen
import dev.danielkindl.ocho.ui.setup.SetupScreen
import dev.danielkindl.ocho.ui.tabata.session.TabataSessionScreen
import dev.danielkindl.ocho.ui.tabata.setup.TabataSetupScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_SETUP = "setup"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LICENSES = "licenses"
private const val ROUTE_SESSION = "session/{totalDurationMillis}/{intervalMillis}"
private const val ROUTE_TABATA_SETUP = "tabata-setup"
private const val ROUTE_TABATA_SESSION = "tabata-session/{totalDurationMillis}/{workMillis}/{restMillis}"

/**
 * The whole navigation graph: home, both setup screens, both session screens, settings.
 *
 * Session configuration travels as route arguments rather than shared state, so each
 * session view model reads its own durations from `SavedStateHandle` and survives
 * rotation without any extra save/restore code.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {

        composable(ROUTE_HOME) {
            HomeScreen(
                onOpenEmom = { navController.navigate(ROUTE_SETUP) },
                onOpenTabata = { navController.navigate(ROUTE_TABATA_SETUP) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }

        composable(ROUTE_SETUP) {
            SetupScreen(
                onStartSession = { totalMs, intervalMs ->
                    navController.navigate("session/$totalMs/$intervalMs")
                },
                onNavigateUp = { navController.navigateUp() },
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onOpenLicenses = { navController.navigate(ROUTE_LICENSES) },
            )
        }

        composable(ROUTE_LICENSES) {
            LicensesScreen(onNavigateUp = { navController.navigateUp() })
        }

        composable(
            route = ROUTE_SESSION,
            arguments = listOf(
                navArgument("totalDurationMillis") { type = NavType.LongType },
                navArgument("intervalMillis") { type = NavType.LongType },
            ),
        ) {
            ActiveSessionScreen(
                onSessionFinished = {
                    navController.popBackStack(ROUTE_SETUP, inclusive = false)
                },
            )
        }

        composable(ROUTE_TABATA_SETUP) {
            TabataSetupScreen(
                onStartSession = { totalMs, workMs, restMs ->
                    navController.navigate("tabata-session/$totalMs/$workMs/$restMs")
                },
                onNavigateUp = { navController.navigateUp() },
            )
        }

        composable(
            route = ROUTE_TABATA_SESSION,
            arguments = listOf(
                navArgument("totalDurationMillis") { type = NavType.LongType },
                navArgument("workMillis") { type = NavType.LongType },
                navArgument("restMillis") { type = NavType.LongType },
            ),
        ) {
            TabataSessionScreen(
                onSessionFinished = {
                    navController.popBackStack(ROUTE_TABATA_SETUP, inclusive = false)
                },
            )
        }
    }
}
