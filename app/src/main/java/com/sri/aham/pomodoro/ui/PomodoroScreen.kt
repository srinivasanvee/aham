package com.sri.aham.pomodoro.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sri.aham.pomodoro.viewmodel.PomodoroPhase
import com.sri.aham.pomodoro.viewmodel.PomodoroUiState
import com.sri.aham.pomodoro.viewmodel.PomodoroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(vm: PomodoroViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro") },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroSection(state = state, vm = vm)
            SettingsSection(state = state, vm = vm)
        }
    }
}

// ---------------------------------------------------------------------------
// Hero section — gradient + timer + controls
// ---------------------------------------------------------------------------

@Composable
private fun HeroSection(state: PomodoroUiState, vm: PomodoroViewModel) {
    val gradientTop by animateColorAsState(
        targetValue = when (state.phase) {
            PomodoroPhase.WORK -> MaterialTheme.colorScheme.tertiaryContainer
            PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
            PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(600),
        label = "gradientTop",
    )
    val arcColor by animateColorAsState(
        targetValue = when (state.phase) {
            PomodoroPhase.WORK -> MaterialTheme.colorScheme.tertiary
            PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
            PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(600),
        label = "arcColor",
    )
    val surfaceColor = MaterialTheme.colorScheme.surface
    val trackColor = arcColor.copy(alpha = 0.18f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(gradientTop, surfaceColor)),
            )
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CircularTimer(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.totalSeconds,
                phase = state.phase,
                arcColor = arcColor,
                trackColor = trackColor,
            )
            SessionDots(
                sessionsCompleted = state.sessionsCompleted,
                sessionsUntilLongBreak = state.sessionsUntilLongBreak,
                accentColor = arcColor,
            )
            TransportControls(
                isRunning = state.isRunning,
                onStart = vm::start,
                onPause = vm::pause,
                onReset = vm::reset,
                onSkip = vm::skip,
                arcColor = arcColor,
            )
        }
    }
}

@Composable
private fun CircularTimer(
    remainingSeconds: Long,
    totalSeconds: Long,
    phase: PomodoroPhase,
    arcColor: Color,
    trackColor: Color,
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val sweepAngle = 360f * progress
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)
    val phaseLabel = when (phase) {
        PomodoroPhase.WORK -> "Focus"
        PomodoroPhase.SHORT_BREAK -> "Short Break"
        PomodoroPhase.LONG_BREAK -> "Long Break"
    }
    val strokeWidth = 16.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Track ring
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            // Progress arc
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun SessionDots(
    sessionsCompleted: Int,
    sessionsUntilLongBreak: Int,
    accentColor: Color,
) {
    val filled = sessionsCompleted % sessionsUntilLongBreak
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(sessionsUntilLongBreak) { index ->
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(
                    color = if (index < filled) accentColor else accentColor.copy(alpha = 0.25f),
                )
            }
        }
    }
}

@Composable
private fun TransportControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    arcColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FilledTonalIconButton(
            onClick = onReset,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Reset")
        }

        FilledIconButton(
            onClick = if (isRunning) onPause else onStart,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = arcColor,
            ),
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                modifier = Modifier.size(36.dp),
            )
        }

        FilledTonalIconButton(
            onClick = onSkip,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Outlined.SkipNext, contentDescription = "Skip")
        }
    }
}

// ---------------------------------------------------------------------------
// Settings section
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSection(state: PomodoroUiState, vm: PomodoroViewModel) {
    val enabled = !state.isRunning

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = "Timer Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        HorizontalDivider()

        DurationRow(
            label = "Work",
            value = state.workMinutes,
            enabled = enabled,
            onDecrement = { vm.setWorkMinutes(state.workMinutes - 1) },
            onIncrement = { vm.setWorkMinutes(state.workMinutes + 1) },
        )

        HorizontalDivider()

        DurationRow(
            label = "Short Break",
            value = state.shortBreakMinutes,
            enabled = enabled,
            onDecrement = { vm.setShortBreakMinutes(state.shortBreakMinutes - 1) },
            onIncrement = { vm.setShortBreakMinutes(state.shortBreakMinutes + 1) },
        )

        HorizontalDivider()

        DurationRow(
            label = "Long Break",
            value = state.longBreakMinutes,
            enabled = enabled,
            onDecrement = { vm.setLongBreakMinutes(state.longBreakMinutes - 1) },
            onIncrement = { vm.setLongBreakMinutes(state.longBreakMinutes + 1) },
        )

        HorizontalDivider()

        Spacer(Modifier.height(8.dp))

        if (!enabled) {
            Text(
                text = "Pause the timer to adjust durations",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DurationRow(
    label: String,
    value: Int,
    enabled: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )

        FilledTonalIconButton(
            onClick = onDecrement,
            enabled = enabled,
            modifier = Modifier.size(36.dp),
        ) {
            Text("−", fontSize = 18.sp)
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "$value min",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp),
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )

        Spacer(Modifier.width(8.dp))

        FilledTonalIconButton(
            onClick = onIncrement,
            enabled = enabled,
            modifier = Modifier.size(36.dp),
        ) {
            Text("+", fontSize = 18.sp)
        }
    }
}
