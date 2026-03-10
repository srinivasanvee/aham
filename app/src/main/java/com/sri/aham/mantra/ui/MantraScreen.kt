package com.sri.aham.mantra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sri.aham.mantra.model.Mantra
import com.sri.aham.mantra.viewmodel.MantraUiState
import com.sri.aham.mantra.viewmodel.MantraViewModel

/**
 * Root composable for the Mantra / Meditation Player feature.
 *
 * Connects to [MantraViewModel] which in turn controls [MantraPlayerService].
 * The connection is started in a [DisposableEffect] so it is tied to the screen's
 * composition lifetime — disconnecting automatically when the user navigates away
 * while leaving the service (and any active playback) running in the background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MantraScreen(viewModel: MantraViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Connect to MantraPlayerService while this screen is in the composition.
    DisposableEffect(Unit) {
        viewModel.connect()
        onDispose { viewModel.disconnect() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mantra Player") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            NowPlayingCard(uiState = uiState)
            TransportControls(
                uiState = uiState,
                onPlay = viewModel::play,
                onPause = viewModel::pause,
                onStop = viewModel::stop,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MantraList(
                mantras = uiState.mantras,
                selected = uiState.selected,
                onSelect = viewModel::selectAndPlay,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Now-playing card
// ---------------------------------------------------------------------------

/**
 * Displays the selected mantra's title, description, and current playback status.
 * Shows a placeholder prompt when nothing is selected yet.
 */
@Composable
private fun NowPlayingCard(uiState: MantraUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = uiState.selected?.title ?: "Select a mantra below",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.selected?.description
                    ?: "Tap a mantra to begin looped playback.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            val statusText = when {
                !uiState.isReady   -> "Connecting…"
                uiState.isPlaying  -> "Playing  ∞  loop"
                uiState.selected != null -> "Paused"
                else               -> "—"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Transport controls  ▶ / ⏸ / ⏹
// ---------------------------------------------------------------------------

/**
 * Play/Pause toggle and Stop button.
 * Buttons are disabled until [MantraUiState.isReady] is true and a mantra is selected.
 */
@Composable
private fun TransportControls(
    uiState: MantraUiState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val enabled = uiState.isReady && uiState.selected != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Primary: Play / Pause toggle
        FilledIconButton(
            onClick = if (uiState.isPlaying) onPause else onPlay,
            enabled = enabled,
            modifier = Modifier.size(64.dp),
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
            )
        }

        // Secondary: Stop (rewinds to start)
        FilledTonalIconButton(
            onClick = onStop,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Mantra selection list
// ---------------------------------------------------------------------------

/**
 * Scrollable list of available mantras.
 * Uses radio-button semantics so accessibility services announce the selection state.
 */
@Composable
private fun MantraList(
    mantras: List<Mantra>,
    selected: Mantra?,
    onSelect: (Mantra) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Mantras",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(mantras, key = { it.id }) { mantra ->
            MantraRow(
                mantra = mantra,
                isSelected = mantra.id == selected?.id,
                hasAudio = mantra.audioRes != null,
                onClick = { onSelect(mantra) },
            )
        }
    }
}

/**
 * Single row in the mantra list.
 *
 * @param hasAudio When false, shows a "No audio" badge reminding the developer to
 *                 add the audio file to res/raw/. Remove this badge once files are added.
 */
@Composable
private fun MantraRow(
    mantra: Mantra,
    isSelected: Boolean,
    hasAudio: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // onClick = null: click is handled by the parent selectable modifier
            RadioButton(selected = isSelected, onClick = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mantra.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = mantra.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Developer hint — remove once audio files are in res/raw/
            if (!hasAudio) {
                Text(
                    text = "No audio",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
