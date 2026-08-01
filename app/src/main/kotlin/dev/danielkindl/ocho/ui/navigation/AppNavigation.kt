package dev.danielkindl.ocho.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.WorkoutMode
import dev.danielkindl.ocho.ui.home.HomeScreen
import dev.danielkindl.ocho.ui.licenses.LicensesScreen
import dev.danielkindl.ocho.ui.session.SessionScreen
import dev.danielkindl.ocho.ui.settings.SettingsScreen
import dev.danielkindl.ocho.ui.setup.WorkoutSetupScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LICENSES = "licenses"

/**
 * One setup destination for every mode, rather than one per mode.
 *
 * Three modes across separate setup and session routes would have meant six
 * destinations to keep in step. The mode travels as an argument instead.
 */
private const val ROUTE_SETUP = "setup/{mode}"

/**
 * One session destination for every mode.
 *
 * `first` and `second` carry whatever the mode needs: interval for EMOM, work and
 * rest for Tabata, neither for AMRAP. Deliberately generic names, because naming
 * them after one mode's meaning would mislead in the other two.
 */
private const val ROUTE_SESSION = "session/{mode}/{total}/{first}/{second}"

/** Builds a concrete setup route. */
internal fun setupRoute(mode: WorkoutMode): String = "setup/${mode.name}"

/** Builds a concrete session route from a request, flattening it to arguments. */
internal fun sessionRoute(request: SessionRequest): String = when (request) {
    is SessionRequest.Emom -> sessionRoute(
        WorkoutMode.EMOM,
        request.config.totalDurationMillis,
        request.config.intervalMillis,
    )

    is SessionRequest.Tabata -> sessionRoute(
        WorkoutMode.TABATA,
        request.config.totalDurationMillis,
        request.config.workMillis,
        request.config.restMillis,
    )

    is SessionRequest.Amrap -> sessionRoute(
        WorkoutMode.AMRAP,
        request.config.totalDurationMillis,
    )
}

private fun sessionRoute(
    mode: WorkoutMode,
    total: Long,
    first: Long = 0L,
    second: Long = 0L,
): String = "session/${mode.name}/$total/$first/$second"

/**
 * The whole navigation graph: home, setup, session, settings and licences.
 *
 * Session configuration travels as route arguments rather than shared state, so the
 * session view model reads its durations from `SavedStateHandle` and survives
 * rotation without any extra save and restore code.
 */
@Composable
fun AppNavigation(activeSessionViewModel: ActiveSessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    // Open straight into a workout that is already running, which is what happens
    // when the user taps the ongoing notification. Keyed on Unit so it runs once:
    // re-navigating on every state change would trap the user on the session screen.
    LaunchedEffect(Unit) {
        activeSessionViewModel.activeSessionRequest()?.let { request ->
            navController.navigate(sessionRoute(request))
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_HOME) {

        composable(ROUTE_HOME) {
            HomeScreen(
                onOpenMode = { mode -> navController.navigate(setupRoute(mode)) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }

        composable(
            route = ROUTE_SETUP,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) {
            WorkoutSetupScreen(
                onStartSession = { request -> navController.navigate(sessionRoute(request)) },
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
                navArgument("mode") { type = NavType.StringType },
                navArgument("total") { type = NavType.LongType },
                navArgument("first") { type = NavType.LongType },
                navArgument("second") { type = NavType.LongType },
            ),
        ) {
            SessionScreen(
                // Pops the session itself rather than popping back to setup. Resuming
                // from the notification navigates straight here from home, so setup
                // may not be on the stack at all and targeting it would pop nothing.
                onSessionFinished = {
                    navController.popBackStack(ROUTE_SESSION, inclusive = true)
                },
            )
        }
    }
}
