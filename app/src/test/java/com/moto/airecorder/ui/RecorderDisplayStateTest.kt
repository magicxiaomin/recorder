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

        assertEquals("录音中", display.statusLabel)
        assertEquals("00:12", display.timerText)
        assertEquals("音频已持续保存", display.supportText)
        assertTrue(display.showRecordingControls)
    }

    @Test
    fun savedStateShowsGenerateNotesCtaAndHidesRecordingControls() {
        val display = RecorderState.Saved(
            recordingId = "demo",
            durationMs = 768_000,
        ).toDisplayState()

        assertEquals("已保存 · 生成纪要", display.statusLabel)
        assertEquals("12:48", display.timerText)
        assertEquals("点按打开 · 12:48", display.supportText)
        assertEquals("生成纪要", display.primaryAction)
        assertFalse(display.showRecordingControls)
    }

    @Test
    fun ringingCallShowsRecordingContinuesCopy() {
        val display = RecorderState.IncomingCallRinging(
            elapsedMs = 12_000,
            markers = emptyList(),
        ).toDisplayState()

        assertEquals("录音中", display.statusLabel)
        assertEquals("来电响铃中 / 录音继续保存", display.supportText)
        assertTrue(display.showRecordingControls)
    }

    @Test
    fun answeredCallShowsPrivacyPauseCopy() {
        val display = RecorderState.PausedForCall(elapsedMs = 12_000).toDisplayState()

        assertEquals("录音已暂停", display.statusLabel)
        assertEquals("通话不录制 / 挂断后自动恢复", display.supportText)
        assertTrue(display.showRecordingControls)
    }
}
