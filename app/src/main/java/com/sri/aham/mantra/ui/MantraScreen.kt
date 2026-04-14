package com.sri.aham.mantra.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sri.aham.mantra.model.Mantra
import com.sri.aham.mantra.model.MantraCategory
import com.sri.aham.mantra.viewmodel.MantraUiState
import com.sri.aham.mantra.viewmodel.MantraViewModel
import com.sri.aham.mantra.viewmodel.SleepTimerOptions

// ---------------------------------------------------------------------------
// Root screen composable
// ---------------------------------------------------------------------------

/**
 * Mantra / Meditation Player screen.
 *
 * Layout: two vertically-stacked sections.
 *  1. **Hero** (top ~60%) — Om mandala animation, now-playing info, transport
 *     controls, and sleep-timer row. Background is a gradient drawn from
 *     `primaryContainer` to `surface`.
 *  2. **Mantra list** (bottom ~40%) — scrollable radio-button list of available
 *     mantras. Tapping a row loads and plays that mantra.
 *
 * A [DisposableEffect] drives [MantraViewModel.connect] / [MantraViewModel.disconnect]
 * so the [MediaController] lifetime matches the screen's composition lifetime.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MantraScreen(viewModel: MantraViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        viewModel.connect()
        onDispose { viewModel.disconnect() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mantra Player") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // ── Tab row ───────────────────────────────────────────────────
            MantraTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::setTab,
            )

            // ── Hero section ──────────────────────────────────────────────
            HeroSection(
                uiState = uiState,
                onPlay  = viewModel::play,
                onPause = viewModel::pause,
                onStop  = viewModel::stop,
                onSetTimer   = viewModel::setTimer,
                onCancelTimer = viewModel::cancelTimer,
                modifier = Modifier.weight(0.58f),
            )

            HorizontalDivider()

            // ── Track list ────────────────────────────────────────────────
            MantraList(
                mantras  = uiState.mantras,
                selected = uiState.selected,
                onSelect = viewModel::selectAndPlay,
                modifier = Modifier.weight(0.42f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tab row
// ---------------------------------------------------------------------------

private val tabs = listOf(MantraCategory.MANTRA, MantraCategory.GUIDED, MantraCategory.SLOKHA)

private fun MantraCategory.label() = when (this) {
    MantraCategory.MANTRA  -> "Mantra"
    MantraCategory.GUIDED  -> "Guided"
    MantraCategory.SLOKHA  -> "Slokha"
}

@Composable
private fun MantraTabRow(
    selectedTab: MantraCategory,
    onTabSelected: (MantraCategory) -> Unit,
) {
    TabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
        tabs.forEach { category ->
            Tab(
                selected = selectedTab == category,
                onClick  = { onTabSelected(category) },
                text     = { Text(category.label()) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hero section
// ---------------------------------------------------------------------------

@Composable
private fun HeroSection(
    uiState: MantraUiState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSetTimer: (Int?) -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.surface,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Pulsing Om mandala
            OmMandala(
                isPlaying = uiState.isPlaying,
                modifier  = Modifier.size(160.dp),
            )

            // Now-playing info
            NowPlayingInfo(uiState = uiState)

            // Transport controls
            TransportControls(
                uiState  = uiState,
                onPlay   = onPlay,
                onPause  = onPause,
                onStop   = onStop,
            )

            // Sleep timer
            SleepTimerRow(
                uiState       = uiState,
                onSetTimer    = onSetTimer,
                onCancelTimer = onCancelTimer,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Om mandala animation
// ---------------------------------------------------------------------------

/** Sine-like easing for the organic pulse feel. */
private val SineInOut = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/**
 * Animated concentric-circle mandala with the Om symbol (ॐ) at the centre.
 *
 * Three circles pulse at slightly offset durations (2 s / 2.5 s / 3 s) to
 * produce a natural breathing-like rhythm. Animation is gated by [isPlaying]:
 * when paused the circles settle to a calm resting size.
 */
@Composable
private fun OmMandala(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface    = MaterialTheme.colorScheme.onSurface

    val transition = rememberInfiniteTransition(label = "mandala")

    val pulse1 by transition.animateFloat(
        initialValue  = 0.72f, targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(2000, easing = SineInOut), RepeatMode.Reverse),
        label = "p1",
    )
    val pulse2 by transition.animateFloat(
        initialValue  = 0.80f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(2500, easing = SineInOut), RepeatMode.Reverse),
        label = "p2",
    )
    val pulse3 by transition.animateFloat(
        initialValue  = 0.88f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(3000, easing = SineInOut), RepeatMode.Reverse),
        label = "p3",
    )

    // When paused, freeze at a calm steady state.
    val s1 = if (isPlaying) pulse1 else 0.82f
    val s2 = if (isPlaying) pulse2 else 0.90f
    val s3 = if (isPlaying) pulse3 else 0.98f

    // Outer circle alpha breathes gently with playback.
    val outerAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.30f else 0.12f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "outerAlpha",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = minOf(cx, cy)

            // Outermost ring
            drawCircle(
                color  = primaryColor,
                radius = maxR * s3,
                style  = Stroke(width = 1.5.dp.toPx()),
                alpha  = outerAlpha,
            )
            // Middle ring
            drawCircle(
                color  = primaryColor,
                radius = maxR * s2,
                style  = Stroke(width = 2.dp.toPx()),
                alpha  = outerAlpha + 0.15f,
            )
            // Inner ring
            drawCircle(
                color  = primaryColor,
                radius = maxR * s1 * 0.72f,
                style  = Stroke(width = 3.dp.toPx()),
                alpha  = outerAlpha + 0.28f,
            )
            // Solid core disc
            drawCircle(
                color  = primaryColor,
                radius = maxR * 0.34f,
                alpha  = 0.12f,
            )
        }

        // OM symbol — rendered as a Text so no asset is needed
        Text(
            text  = "ॐ",
            fontSize  = 52.sp,
            color     = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ---------------------------------------------------------------------------
// Now-playing info + loop counter
// ---------------------------------------------------------------------------

/**
 * Shows the selected mantra's name, description, playback status, and loop count.
 * Uses [AnimatedContent] so title/description crossfade when the selection changes.
 */
@Composable
private fun NowPlayingInfo(uiState: MantraUiState) {
    AnimatedContent(
        targetState = uiState.selected,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "nowPlaying",
    ) { mantra ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text      = mantra?.title ?: "Select a mantra",
                style     = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = mantra?.description ?: "Choose from the list below to begin",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    // Status + loop counter row
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        val statusColor by animateColorAsState(
            targetValue = if (uiState.isPlaying)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            label = "statusColor",
        )
        val statusText = when {
            !uiState.isReady          -> "Connecting…"
            uiState.isFadingOut       -> "Fading out…"
            uiState.isPlaying         -> "▶  Playing  ∞"
            uiState.selected != null  -> "⏸  Paused"
            else                      -> "—"
        }
        Text(
            text  = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
        )

        if (uiState.loopCount > 0) {
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text  = "Loop ${uiState.loopCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Transport controls
// ---------------------------------------------------------------------------

/**
 * Play/Pause and Stop buttons. Disabled until the controller is ready and a
 * mantra is selected. The Stop button fades when the timer is fading out to
 * give a visual hint that the session is ending automatically.
 */
@Composable
private fun TransportControls(
    uiState: MantraUiState,
    onPlay:  () -> Unit,
    onPause: () -> Unit,
    onStop:  () -> Unit,
) {
    val enabled = uiState.isReady && uiState.selected != null

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.fillMaxWidth(),
    ) {
        // Stop
        FilledTonalIconButton(
            onClick  = onStop,
            enabled  = enabled,
            modifier = Modifier.size(52.dp),
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop")
        }

        // Play / Pause — primary larger button
        FilledIconButton(
            onClick  = if (uiState.isPlaying) onPause else onPlay,
            enabled  = enabled,
            modifier = Modifier.size(72.dp),
            colors   = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector    = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                modifier       = Modifier.size(40.dp),
            )
        }

        // Spacer to keep Play centred visually
        Spacer(Modifier.size(52.dp))
    }
}

// ---------------------------------------------------------------------------
// Sleep timer
// ---------------------------------------------------------------------------

/**
 * Sleep-timer controls shown below the transport buttons.
 *
 * When no timer is set: a [TextButton] showing "Sleep timer" opens a
 * [DropdownMenu] to choose a duration.
 *
 * When a timer is running: a countdown label + [LinearProgressIndicator] are
 * shown alongside a cancel icon. During fade-out the progress bar colour
 * shifts to the error colour as a visual warning.
 */
@Composable
private fun SleepTimerRow(
    uiState: MantraUiState,
    onSetTimer: (Int?) -> Unit,
    onCancelTimer: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (uiState.timerRemainingSeconds != null) {
            // ── Timer running ────────────────────────────────────────────
            val totalSeconds = (uiState.timerMinutes ?: 1) * 60f
            val progress = uiState.timerRemainingSeconds / totalSeconds

            val progressColor by animateColorAsState(
                targetValue = if (uiState.isFadingOut)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                label = "progressColor",
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = formatCountdown(uiState.timerRemainingSeconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalIconButton(
                    onClick  = onCancelTimer,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Outlined.TimerOff,
                        contentDescription = "Cancel timer",
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(3.dp),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f),
            )
        } else {
            // ── Timer picker ─────────────────────────────────────────────
            Box {
                TextButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .alpha(0.6f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Sleep timer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    SleepTimerOptions.forEach { minutes ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (minutes == null) "No timer"
                                    else "$minutes min",
                                )
                            },
                            onClick = {
                                onSetTimer(minutes)
                                menuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Formats [seconds] as `mm:ss` (or `h:mm:ss` for durations ≥ 1 hour). */
private fun formatCountdown(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// ---------------------------------------------------------------------------
// Mantra list
// ---------------------------------------------------------------------------

/**
 * Scrollable radio-button list of available mantras.
 * Tapping a row calls [onSelect] which loads and plays that mantra.
 */
@Composable
private fun MantraList(
    mantras: List<Mantra>,
    selected: Mantra?,
    onSelect: (Mantra) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (mantras.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "Coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(mantras, key = { it.id }) { mantra ->
            MantraRow(
                mantra     = mantra,
                isSelected = mantra.id == selected?.id,
                hasAudio   = mantra.audioRes != null,
                onClick    = { onSelect(mantra) },
            )
        }
    }
}

/**
 * Single row in the mantra list.
 *
 * The row uses [Role.RadioButton] so TalkBack reads "Om, radio button, selected / not selected".
 * When [hasAudio] is false a small "Add audio" label is shown as a developer reminder.
 */
@Composable
private fun MantraRow(
    mantra: Mantra,
    isSelected: Boolean,
    hasAudio: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(250),
        label = "rowColor",
    )

    Surface(
        color  = containerColor,
        shape  = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick  = onClick,
                role     = Role.RadioButton,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(selected = isSelected, onClick = null)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = mantra.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text  = mantra.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Developer hint — remove badge once audio files are in res/raw/
            if (!hasAudio) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text     = "Add audio",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
