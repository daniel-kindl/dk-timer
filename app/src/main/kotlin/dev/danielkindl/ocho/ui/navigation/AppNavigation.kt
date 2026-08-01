package dev.danielkindl.ocho.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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

/** Builds a concrete EMOM session route. */
internal fun sessionRoute(totalMillis: Long, intervalMillis: Long): String =
    "session/$totalMillis/$intervalMillis"

/** Builds a concrete Tabata session route. */
internal fun tabataSessionRoute(totalMillis: Long, workMillis: Long, restMillis: Long): String =
    "tabata-session/$totalMillis/$workMillis/$restMillis"

/**
 * The whole navigation graph: home, both setup screens, both session screens, settings.
 *
 * Session configuration travels as route arguments rather than shared state, so each
 * session view model reads its own durations from `SavedStateHandle` and survives
 * rotation without any extra save/restore code.
 */
@Composable
fun AppNavigation(activeSessionViewModel: ActiveSessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    // Open straight into a workout that is already running, which is what happens
    // when the user taps the ongoing notification. Keyed on Unit so it runs once:
    // re-navigating on every state change would trap the user on the session screen.
    LaunchedEffect(Unit) {
        activeSessionViewModel.activeSessionRoute()?.let { route ->
            navController.navigate(route)
        }
    }

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
                    navController.navigate(sessionRoute(totalMs, intervalMs))
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
                // Pops the session itself rather than popping back to setup. Resuming
                // from the notification navigates straight here from home, so setup
                // may not be on the stack at all and targeting it would pop nothing.
                onSessionFinished = {
                    navController.popBackStack(ROUTE_SESSION, inclusive = true)
                },
            )
        }

        composable(ROUTE_TABATA_SETUP) {
            TabataSetupScreen(
                onStartSession = { totalMs, workMs, restMs ->
                    navController.navigate(tabataSessionRoute(totalMs, workMs, restMs))
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
                // See the EMOM session above: pop this destination, not its setup screen.
                onSessionFinished = {
                    navController.popBackStack(ROUTE_TABATA_SESSION, inclusive = true)
                },
            )
        }
    }
}
