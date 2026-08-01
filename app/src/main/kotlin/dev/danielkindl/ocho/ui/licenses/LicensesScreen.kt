package dev.danielkindl.ocho.ui.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.danielkindl.ocho.R

/**
 * Displays the bundled third-party licence texts.
 *
 * This screen is not a courtesy — it is how the app meets its obligations. The
 * embedded fonts are under the SIL Open Font License 1.1 and the icons under ISC,
 * and both require that the copyright and licence notices accompany every copy of
 * the software. The APK is the copy users receive, so the notices have to be
 * readable from inside it, not only from the repository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current

    // Read once and hold: the file is a few kilobytes and never changes, so
    // re-reading it on every recomposition would be pure waste.
    val notices = remember {
        context.resources.openRawResource(R.raw.third_party_notices)
            .bufferedReader()
            .use { it.readText() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Licences") },
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Monospaced, because licence texts are hard-wrapped at 80 columns and
            // reflowing them in a proportional face makes them noticeably harder to read.
            Text(
                text = notices,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = dev.danielkindl.ocho.ui.theme.JetBrainsMonoFamily,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
