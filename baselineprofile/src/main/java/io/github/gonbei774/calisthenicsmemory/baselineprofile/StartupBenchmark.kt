package io.github.gonbei774.calisthenicsmemory.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.os.Build
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start and destination switching, without and with the Baseline Profile, so the gain is
 * measured rather than assumed. Numbers from an emulator are only comparable with each other.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test fun coldStartWithoutProfile() = coldStart(CompilationMode.None())

    @Test fun coldStartWithProfile() = coldStart(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test fun destinationsWithoutProfile() = destinations(CompilationMode.None())

    @Test fun destinationsWithProfile() = destinations(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun coldStart(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = 10,
    ) {
        pressHome()
        startActivityAndWait()
        waitForToday()
    }

    private fun destinations(mode: CompilationMode) {
        // Frame timing needs the frame timeline in Perfetto traces (API 31+); API 29 records none.
        assumeTrue(Build.VERSION.SDK_INT >= 31)
        rule.measureRepeated(
            packageName = PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = mode,
            startupMode = StartupMode.WARM,
            iterations = 10,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                waitForToday()
            },
        ) {
            visitDestinations()
        }
    }
}
