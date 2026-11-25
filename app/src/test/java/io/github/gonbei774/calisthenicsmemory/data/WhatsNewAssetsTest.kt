package io.github.gonbei774.calisthenicsmemory.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The build bundles the changelog and the store summaries as assets, and the app can read them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WhatsNewAssetsTest {
    private val assets get() = ApplicationProvider.getApplicationContext<Application>().assets

    @Test fun `the bundled changelog lists the installed version on top`() {
        val shown = WhatsNewAssets.releases(assets, BuildConfig.VERSION_NAME)
        assertTrue(shown.isNotEmpty())
        assertEquals(BuildConfig.VERSION_NAME, shown.first { it.isKalimory }.version)
        assertTrue(shown.any { !it.isKalimory })
    }

    @Test fun `the installed version has a store summary to open the sheet with`() {
        val summary = WhatsNewAssets.summary(assets, BuildConfig.VERSION_CODE)
        assertNotNull(summary)
        assertTrue(summary!!.length <= 500)
    }

    @Test fun `a missing asset gives nothing rather than an error`() {
        assertEquals(null, WhatsNewAssets.summary(assets, 999_999))
    }
}
