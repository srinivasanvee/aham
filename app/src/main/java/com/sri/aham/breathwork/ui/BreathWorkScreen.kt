package com.sri.aham.breathwork.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sri.aham.breathwork.model.BreathCue
import com.sri.aham.breathwork.model.BreathWorkPhase
import com.sri.aham.breathwork.model.BreathWorkState
import com.sri.aham.breathwork.viewmodel.BreathWorkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathWorkScreen(vm: BreathWorkViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Breath Work") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SafetyCard()
            Text(
                text = "Round ${state.round} of ${state.totalRounds}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            PhaseVisualizer(state)
            Instructions(state)
            Controls(state, vm)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SafetyCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(
            text = "Safety: practice seated or lying down in a safe place. Never practice in water, while driving, or anywhere a loss of consciousness could cause harm. Stop if you feel unwell.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun PhaseVisualizer(state: BreathWorkState) {
    val targetScale = when {
        state.phase == BreathWorkPhase.BREATHING && state.cue == BreathCue.INHALE -> 1f
        state.phase == BreathWorkPhase.BREATHING -> 0.62f
        else -> 0.78f
    }
    val scale by animateFloatAsState(targetScale, tween(1_800), label = "breathScale")
    val accent = when (state.phase) {
        BreathWorkPhase.EXHALE_HOLD -> MaterialTheme.colorScheme.tertiary
        BreathWorkPhase.RECOVERY_HOLD -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(accent.copy(alpha = 0.12f), radius = size.minDimension * 0.48f)
            drawCircle(accent.copy(alpha = 0.25f), radius = size.minDimension * 0.38f * scale)
            drawCircle(accent, radius = size.minDimension * 0.27f * scale, style = Stroke(7.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = phaseTitle(state),
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = phaseValue(state),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}

private fun phaseTitle(state: BreathWorkState): String = when (state.phase) {
    BreathWorkPhase.READY -> "Ready"
    BreathWorkPhase.BREATHING -> if (state.cue == BreathCue.INHALE) "Breathe in" else "Let go"
    BreathWorkPhase.EXHALE_HOLD -> "Hold after exhale"
    BreathWorkPhase.RECOVERY_HOLD -> "Recovery hold"
    BreathWorkPhase.COMPLETE -> "Complete"
}

private fun phaseValue(state: BreathWorkState): String = when (state.phase) {
    BreathWorkPhase.READY -> ""
    BreathWorkPhase.BREATHING -> "${state.breathNumber}/${state.breathsPerRound}"
    BreathWorkPhase.EXHALE_HOLD -> formatSeconds(state.holdSecondsElapsed)
    BreathWorkPhase.RECOVERY_HOLD -> state.phaseSecondsRemaining.toString()
    BreathWorkPhase.COMPLETE -> "✓"
}

private fun formatSeconds(total: Int): String = "%d:%02d".format(total / 60, total % 60)

@Composable
private fun Instructions(state: BreathWorkState) {
    val text = when (state.phase) {
        BreathWorkPhase.READY -> "Three rounds: 30 deep breaths, a comfortable exhale hold, then a 15-second recovery hold."
        BreathWorkPhase.BREATHING -> "Breathe deeply without forcing. Keep your body relaxed."
        BreathWorkPhase.EXHALE_HOLD -> "Stay relaxed. Tap when you feel the natural urge to breathe—never force the hold."
        BreathWorkPhase.RECOVERY_HOLD -> "Inhale fully and hold gently until the countdown finishes."
        BreathWorkPhase.COMPLETE -> "Session complete. Breathe normally and take a moment before standing."
    }
    Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun Controls(state: BreathWorkState, vm: BreathWorkViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state.phase) {
            BreathWorkPhase.READY -> Button(onClick = vm::start) { Text("Begin session") }
            BreathWorkPhase.BREATHING -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(onClick = if (state.isRunning) vm::pause else vm::start) {
                        Icon(
                            if (state.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (state.isRunning) "Pause" else "Resume",
                        )
                    }
                    OutlinedButton(onClick = vm::finishBreathing) { Text("Begin hold") }
                }
            }
            BreathWorkPhase.EXHALE_HOLD -> {
                FilledTonalButton(onClick = vm::releaseHold) { Text("I need to breathe") }
                OutlinedButton(onClick = if (state.isRunning) vm::pause else vm::start) {
                    Text(if (state.isRunning) "Pause timer" else "Resume timer")
                }
            }
            BreathWorkPhase.RECOVERY_HOLD -> OutlinedButton(onClick = if (state.isRunning) vm::pause else vm::start) {
                Text(if (state.isRunning) "Pause" else "Resume")
            }
            BreathWorkPhase.COMPLETE -> Button(onClick = vm::reset) { Text("Start again") }
        }
        if (state.phase != BreathWorkPhase.READY && state.phase != BreathWorkPhase.COMPLETE) {
            OutlinedButton(onClick = vm::reset) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Text(" Reset")
            }
        }
    }
}
