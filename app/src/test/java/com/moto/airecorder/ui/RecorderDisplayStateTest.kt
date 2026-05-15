package com.moto.airecorder.ui

import com.moto.airecorder.domain.RecorderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecorderDisplayStateTest {
    @Test
    fun recordingStateShowsRecTimerAndControls() {
        val display = RecorderState.Recording(
            elapsedMs = 12_000,
            markers = emptyList(),
        ).toDisplayState()

        assertEquals("REC", display.statusLabel)
        assertEquals("00:12", display.timerText)
        assertEquals("Audio saved continuously", display.supportText)
        assertTrue(display.showRecordingControls)
    }

    @Test
    fun savedStateShowsGenerateNotesCtaAndHidesRecordingControls() {
        val display = RecorderState.Saved(
            recordingId = "demo",
            durationMs = 768_000,
        ).toDisplayState()

        assertEquals("Saved · Generate notes", display.statusLabel)
        assertEquals("12:48", display.timerText)
        assertEquals("Tap to open · 12:48", display.supportText)
        assertEquals("Generate notes", display.primaryAction)
        assertFalse(display.showRecordingControls)
    }
}
