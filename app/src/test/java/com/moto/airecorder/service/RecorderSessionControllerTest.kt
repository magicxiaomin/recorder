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

        override suspend fun start(): ActiveRecording {
            starts += 1
            return ActiveRecording(filePath = "test.m4a")
        }

        override suspend fun stop(): SavedRecording {
            stops += 1
            return SavedRecording(recordingId = recordingId, filePath = "test.m4a")
        }

        override suspend fun pause() = Unit

        override suspend fun resume() = Unit
    }
}
