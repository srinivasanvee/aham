package com.sri.aham.breathwork.model

enum class BreathWorkPhase { READY, BREATHING, EXHALE_HOLD, RECOVERY_HOLD, COMPLETE }

enum class BreathCue { INHALE, EXHALE }

data class BreathWorkState(
    val phase: BreathWorkPhase = BreathWorkPhase.READY,
    val cue: BreathCue = BreathCue.INHALE,
    val round: Int = 1,
    val totalRounds: Int = 3,
    val breathNumber: Int = 0,
    val breathsPerRound: Int = 30,
    val phaseSecondsRemaining: Int = 0,
    val holdSecondsElapsed: Int = 0,
    val isRunning: Boolean = false,
    val cueSeconds: Int = 2,
    val retentionHistory: List<Int> = emptyList(),
)

/** Pure state machine for the guided breathing protocol. */
class BreathWorkSession {
    fun start(state: BreathWorkState = BreathWorkState()): BreathWorkState = when (state.phase) {
        BreathWorkPhase.READY -> state.copy(
            phase = BreathWorkPhase.BREATHING,
            cue = BreathCue.INHALE,
            breathNumber = 1,
            phaseSecondsRemaining = state.cueSeconds,
            isRunning = true,
        )
        BreathWorkPhase.BREATHING,
        BreathWorkPhase.EXHALE_HOLD,
        BreathWorkPhase.RECOVERY_HOLD -> state.copy(isRunning = true)
        BreathWorkPhase.COMPLETE -> state
    }

    fun pause(state: BreathWorkState): BreathWorkState = state.copy(isRunning = false)

    fun reset(state: BreathWorkState = BreathWorkState()): BreathWorkState =
        BreathWorkState(
            totalRounds = state.totalRounds,
            cueSeconds = state.cueSeconds,
            breathsPerRound = state.breathsPerRound.coerceIn(MIN_BREATHS_PER_ROUND, MAX_BREATHS_PER_ROUND),
        )

    fun setBreathsPerRound(state: BreathWorkState, breaths: Int): BreathWorkState {
        if (state.phase != BreathWorkPhase.READY) return state
        return state.copy(
            breathsPerRound = breaths.coerceIn(MIN_BREATHS_PER_ROUND, MAX_BREATHS_PER_ROUND),
        )
    }

    fun configure(state: BreathWorkState, rounds: Int, cueSeconds: Int): BreathWorkState {
        if (state.phase != BreathWorkPhase.READY) return state
        return state.copy(totalRounds = rounds.coerceIn(1, 5), cueSeconds = cueSeconds.coerceIn(2, 4))
    }

    fun tick(state: BreathWorkState): BreathWorkState {
        if (!state.isRunning) return state
        return when (state.phase) {
            BreathWorkPhase.BREATHING -> tickBreathing(state)
            BreathWorkPhase.EXHALE_HOLD -> state.copy(holdSecondsElapsed = state.holdSecondsElapsed + 1)
            BreathWorkPhase.RECOVERY_HOLD -> tickRecovery(state)
            BreathWorkPhase.READY, BreathWorkPhase.COMPLETE -> state
        }
    }

    fun finishBreathing(state: BreathWorkState): BreathWorkState {
        if (state.phase != BreathWorkPhase.BREATHING) return state
        return state.copy(
            phase = BreathWorkPhase.EXHALE_HOLD,
            phaseSecondsRemaining = 0,
            holdSecondsElapsed = 0,
            isRunning = true,
        )
    }

    fun releaseHold(state: BreathWorkState): BreathWorkState {
        if (state.phase != BreathWorkPhase.EXHALE_HOLD) return state
        return state.copy(
            phase = BreathWorkPhase.RECOVERY_HOLD,
            retentionHistory = state.retentionHistory + state.holdSecondsElapsed,
            phaseSecondsRemaining = RECOVERY_HOLD_SECONDS,
            isRunning = true,
        )
    }

    private fun tickBreathing(state: BreathWorkState): BreathWorkState {
        if (state.phaseSecondsRemaining > 1) {
            return state.copy(phaseSecondsRemaining = state.phaseSecondsRemaining - 1)
        }
        if (state.cue == BreathCue.INHALE) {
            return state.copy(cue = BreathCue.EXHALE, phaseSecondsRemaining = state.cueSeconds)
        }
        if (state.breathNumber >= state.breathsPerRound) return finishBreathing(state)
        return state.copy(
            cue = BreathCue.INHALE,
            breathNumber = state.breathNumber + 1,
            phaseSecondsRemaining = state.cueSeconds,
        )
    }

    private fun tickRecovery(state: BreathWorkState): BreathWorkState {
        if (state.phaseSecondsRemaining > 1) {
            return state.copy(phaseSecondsRemaining = state.phaseSecondsRemaining - 1)
        }
        if (state.round >= state.totalRounds) {
            return state.copy(
                phase = BreathWorkPhase.COMPLETE,
                phaseSecondsRemaining = 0,
                isRunning = false,
            )
        }
        return state.copy(
            phase = BreathWorkPhase.BREATHING,
            cue = BreathCue.INHALE,
            round = state.round + 1,
            breathNumber = 1,
            phaseSecondsRemaining = state.cueSeconds,
            holdSecondsElapsed = 0,
            isRunning = true,
        )
    }

    companion object {
        const val MIN_BREATHS_PER_ROUND = 5
        const val MAX_BREATHS_PER_ROUND = 60
        const val BREATH_COUNT_STEP = 5
        const val BREATH_CUE_SECONDS = 2
        const val RECOVERY_HOLD_SECONDS = 15
    }
}
