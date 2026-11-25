package io.github.gonbei774.calisthenicsmemory.data

import android.content.res.AssetManager

/** The changelog and the store summaries the build copied into the assets (see CopyWhatsNew in app/build.gradle.kts). */
object WhatsNewAssets {
    /** The releases to show for [versionName]; empty when the file cannot be read. Reads and parses ~60 KB: call off the main thread. */
    fun releases(assets: AssetManager, versionName: String): List<Release> =
        runCatching { Changelog.upTo(Changelog.parse(read(assets, "whatsnew/CHANGELOG.md")), versionName) }.getOrDefault(emptyList())

    /** The short overview written for the store (500 characters at most) of the version with [versionCode], if there is one. */
    fun summary(assets: AssetManager, versionCode: Int): String? =
        runCatching { read(assets, "whatsnew/$versionCode.txt").trim().ifEmpty { null } }.getOrNull()

    private fun read(assets: AssetManager, path: String) = assets.open(path).bufferedReader().use { it.readText() }
}
