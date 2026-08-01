package dev.danielkindl.ocho.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielkindl.ocho.R
import dev.danielkindl.ocho.ui.components.ErrorPlate
import dev.danielkindl.ocho.ui.components.SessionProgressBar

/**
 * Feedback toggles, the in-app updater, and app info.
 *
 * Two view models rather than one: settings are trivial state, while the update flow
 * is a multi-step state machine with its own failure modes, and merging them would
 * put download progress in the same object as a pair of booleans.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ListItem(
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_bell), contentDescription = null)
                },
                headlineContent = { Text("Sound") },
                supportingContent = {
                    Text(
                        "Play a beep at each timer event (uses alarm audio stream, ignores silent mode)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = viewModel::setSoundEnabled,
                    )
                },
            )
            HorizontalDivider()
            ListItem(
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_zap), contentDescription = null)
                },
                headlineContent = { Text("Vibration") },
                supportingContent = {
                    Text(
                        "Vibrate at each timer event and on workout completion",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = viewModel::setVibrationEnabled,
                    )
                },
            )
            HorizontalDivider()
            UpdateSection(
                state = updateState,
                onCheck = updateViewModel::checkForUpdates,
                onDownload = updateViewModel::startDownload,
                onInstall = {
                    if (updateViewModel.canInstallPackages()) {
                        updateViewModel.startInstall()
                    } else {
                        context.startActivity(updateViewModel.unknownSourcesSettingsIntent())
                    }
                },
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()

            Spacer(modifier = Modifier.weight(1f))

            // The wordmark is set in type, not drawn: "ocho", always lowercase, with
            // only the final o in brand green so it reads as the timer dial. Two
            // pre-rendered variants rather than live text, so the exact tracking and
            // the green-o treatment survive font fallback.
            Image(
                painter = painterResource(
                    if (isSystemInDarkTheme()) R.drawable.wordmark_dark else R.drawable.wordmark_light
                ),
                contentDescription = "Ocho",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(40.dp)
                    .padding(bottom = 8.dp),
            )

            val uriHandler = LocalUriHandler.current
            val authorLink = buildAnnotatedString {
                append("Made by ")
                pushStringAnnotation(tag = "URL", annotation = "https://daniel-kindl.github.io/")
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                ) { append("Daniel Kindl") }
                pop()
            }
            ClickableText(
                text = authorLink,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 4.dp),
                onClick = { offset ->
                    authorLink.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()
                        ?.let { uriHandler.openUri(it.item) }
                },
            )
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun UpdateSection(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    when (state) {
        is UpdateUiState.Idle, is UpdateUiState.UpToDate ->
            ListItem(
                headlineContent = { Text("Check for updates") },
                supportingContent = if (state is UpdateUiState.UpToDate) {
                    { Text("You're up to date", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    null
                },
                trailingContent = { TextButton(onClick = onCheck) { Text("Check") } },
            )

        is UpdateUiState.Checking ->
            ListItem(
                headlineContent = { Text("Checking for updates…") },
                trailingContent = { CircularProgressIndicator(modifier = Modifier.height(24.dp)) },
            )

        is UpdateUiState.Available ->
            ListItem(
                headlineContent = { Text("Update available: ${state.update.tagName}") },
                supportingContent = {
                    Text(state.update.releaseNotes, style = MaterialTheme.typography.bodyMedium)
                },
                trailingContent = { Button(onClick = onDownload) { Text("Download") } },
            )

        is UpdateUiState.Downloading ->
            ListItem(
                headlineContent = { Text("Downloading update… ${state.progressPercent}%") },
                supportingContent = {
                    SessionProgressBar(progress = state.progressPercent / PERCENT_MAX)
                },
            )

        is UpdateUiState.ReadyToInstall ->
            ListItem(
                headlineContent = { Text("Update ${state.update.tagName} ready to install") },
                trailingContent = { Button(onClick = onInstall) { Text("Install") } },
            )

        // An error is a plate inside the layout, never a colour applied to the
        // surrounding surface — see ErrorPlate for why that separation is structural.
        is UpdateUiState.Error ->
            ErrorPlate(
                message = state.message,
                actionLabel = "Try again",
                onAction = onCheck,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
    }
}

private const val PERCENT_MAX = 100f
