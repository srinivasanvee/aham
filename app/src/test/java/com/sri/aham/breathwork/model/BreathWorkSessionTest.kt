package com.sri.aham.breathwork.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathWorkSessionTest {
    private val session = BreathWorkSession()

    @Test
    fun startBeginsFirstInhale() {
        val state = session.start()
        assertEquals(BreathWorkPhase.BREATHING, state.phase)
        assertEquals(BreathCue.INHALE, state.cue)
        assertEquals(1, state.breathNumber)
        assertTrue(state.isRunning)
    }

    @Test
    fun breathingAlternatesCueAndAdvancesBreath() {
        var state = session.start()
        repeat(2) { state = session.tick(state) }
        assertEquals(BreathCue.EXHALE, state.cue)
        assertEquals(1, state.breathNumber)
        repeat(2) { state = session.tick(state) }
        assertEquals(BreathCue.INHALE, state.cue)
        assertEquals(2, state.breathNumber)
    }

    @Test
    fun exhaleHoldCountsUpUntilReleased() {
        var state = session.finishBreathing(session.start())
        repeat(73) { state = session.tick(state) }
        assertEquals(BreathWorkPhase.EXHALE_HOLD, state.phase)
        assertEquals(73, state.holdSecondsElapsed)
        state = session.releaseHold(state)
        assertEquals(BreathWorkPhase.RECOVERY_HOLD, state.phase)
        assertEquals(15, state.phaseSecondsRemaining)
    }

    @Test
    fun recoveryHoldAdvancesRoundAndFinalRoundCompletes() {
        var state = session.releaseHold(session.finishBreathing(session.start()))
        repeat(15) { state = session.tick(state) }
        assertEquals(2, state.round)
        assertEquals(BreathWorkPhase.BREATHING, state.phase)

        state = state.copy(round = 3)
        state = session.releaseHold(session.finishBreathing(state))
        repeat(15) { state = session.tick(state) }
        assertEquals(BreathWorkPhase.COMPLETE, state.phase)
        assertFalse(state.isRunning)
    }

    @Test
    fun pausedSessionDoesNotTick() {
        val paused = session.pause(session.start())
        assertEquals(paused, session.tick(paused))
    }

    @Test
    fun breathCountCanBeCustomizedBeforeStarting() {
        val configured = session.setBreathsPerRound(BreathWorkState(), 20)
        var state = session.start(configured)
        repeat(20 * 4) { state = session.tick(state) }
        assertEquals(BreathWorkPhase.EXHALE_HOLD, state.phase)
        assertEquals(20, state.breathNumber)
    }

    @Test
    fun breathCountIsBoundedAndCannotChangeDuringSession() {
        assertEquals(5, session.setBreathsPerRound(BreathWorkState(), -10).breathsPerRound)
        assertEquals(60, session.setBreathsPerRound(BreathWorkState(), 100).breathsPerRound)

        val running = session.start(BreathWorkState(breathsPerRound = 20))
        assertEquals(running, session.setBreathsPerRound(running, 40))
    }

    @Test
    fun resetPreservesCustomizedBreathCount() {
        val configured = BreathWorkState(breathsPerRound = 45)
        assertEquals(45, session.reset(configured).breathsPerRound)
        assertEquals(BreathWorkPhase.READY, session.reset(configured).phase)
    }
}
