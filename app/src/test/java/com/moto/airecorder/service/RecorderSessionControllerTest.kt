package com.moto.airecorder.service

import app.cash.turbine.test
import com.moto.airecorder.domain.RecorderState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecorderSessionControllerTest {
    @Test
    fun startRecording_startsEngineAndPublishesElapsedTimer() = runTest {
        val engine = FakeRecorderEngine()
        val controller = newController(engine)

        controller.state.test {
            assertEquals(RecorderState.Idle, awaitItem())

            controller.start()

            assertTrue(awaitItem() is RecorderState.Recording)
            advanceTimeBy(1_200)
            runCurrent()

            val tick = expectMostRecentItem() as RecorderState.Recording
            assertEquals(1_200, tick.elapsedMs)
            assertEquals(1, engine.starts)
            controller.stopAndSeal()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun markRecording_addsCurrentElapsedMarker() = runTest {
        val controller = newController(FakeRecorderEngine())
        controller.start()
        advanceTimeBy(3_400)
        runCurrent()

        controller.mark()

        val state = controller.state.value as RecorderState.Recording
        assertEquals(listOf(3_400L), state.markers)
        controller.stopAndSeal()
    }

    @Test
    fun stopRecording_sealsThenPublishesSavedRecording() = runTest {
        val engine = FakeRecorderEngine(recordingId = "demo-1")
        val controller = newController(engine)
        controller.start()
        advanceTimeBy(2_500)

        controller.stopAndSeal()

        assertEquals(1, engine.stops)
        assertEquals(RecorderState.Saved(recordingId = "demo-1", durationMs = 2_500), controller.state.value)
    }

    @Test
    fun startAfterSaved_doesNotRestartRecording() = runTest {
        val engine = FakeRecorderEngine(recordingId = "demo-1")
        val controller = newController(engine)
        controller.start()
        advanceTimeBy(1_000)
        controller.stopAndSeal()

        controller.start()

        assertEquals(1, engine.starts)
        assertEquals(RecorderState.Saved(recordingId = "demo-1", durationMs = 1_000), controller.state.value)
    }

    @Test
    fun aiKeyToggle_startsStopsAndCanStartNewRecordingAfterSaved() = runTest {
        val engine = FakeRecorderEngine(recordingId = "demo-1")
        val controller = newController(engine)

        controller.toggleAiKey()
        assertTrue(controller.state.value is RecorderState.Recording)
        assertEquals(1, engine.starts)

        advanceTimeBy(1_500)
        controller.toggleAiKey()
        assertEquals(1, engine.stops)
        assertEquals(RecorderState.Saved(recordingId = "demo-1", durationMs = 1_500), controller.state.value)

        controller.toggleAiKey()
        assertTrue(controller.state.value is RecorderState.Recording)
        assertEquals(2, engine.starts)
        controller.stopAndSeal()
    }

    @Test
    fun ringingCall_doesNotPauseRecordingAndTimerContinues() = runTest {
        val engine = FakeRecorderEngine()
        val controller = newController(engine)
        controller.start()
        advanceTimeBy(1_000)
        runCurrent()

        controller.onIncomingCallRinging()
        advanceTimeBy(750)
        runCurrent()

        val state = controller.state.value as RecorderState.IncomingCallRinging
        assertEquals(1_700, state.elapsedMs)
        assertEquals(0, engine.pauses)
        controller.stopAndSeal()
    }

    @Test
    fun answeredCall_pausesForPrivacyAndResumesAfterCallWithoutCountingCallTime() = runTest {
        val engine = FakeRecorderEngine()
        val controller = newController(engine)
        controller.start()
        advanceTimeBy(2_000)
        runCurrent()
        controller.onIncomingCallRinging()

        controller.onCallAnswered()
        assertEquals(1, engine.pauses)
        assertEquals(RecorderState.PausedForCall(elapsedMs = 2_000), controller.state.value)

        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(RecorderState.PausedForCall(elapsedMs = 2_000), controller.state.value)

        controller.onCallEnded()
        assertEquals(1, engine.resumes)
        assertEquals(RecorderState.Recording(elapsedMs = 2_000, markers = emptyList()), controller.state.value)

        advanceTimeBy(600)
        runCurrent()
        val resumed = controller.state.value as RecorderState.Recording
        assertEquals(2_600, resumed.elapsedMs)
        controller.stopAndSeal()
    }

    private fun TestScope.newController(engine: RecorderEngine): RecorderSessionController {
        return RecorderSessionController(
            engine = engine,
            scope = this,
            dispatcher = StandardTestDispatcher(testScheduler),
            timeProvider = TimeProvider { testScheduler.currentTime },
        )
    }

    private class FakeRecorderEngine(
        private val recordingId: String = "recording-test",
    ) : RecorderEngine {
        var starts = 0
        var stops = 0
        var pauses = 0
        var resumes = 0

        override suspend fun start(): ActiveRecording {
            starts += 1
            return ActiveRecording(filePath = "test.m4a")
        }

        override suspend fun stop(): SavedRecording {
            stops += 1
            return SavedRecording(recordingId = recordingId, filePath = "test.m4a")
        }

        override suspend fun pause() {
            pauses += 1
        }

        override suspend fun resume() {
            resumes += 1
        }
    }
}
