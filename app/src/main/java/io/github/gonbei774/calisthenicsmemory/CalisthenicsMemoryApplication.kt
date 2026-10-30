package io.github.gonbei774.calisthenicsmemory

import android.app.Application
import android.content.Context
import io.github.gonbei774.calisthenicsmemory.data.CrashContext
import io.github.gonbei774.calisthenicsmemory.data.CrashReports
import io.github.gonbei774.calisthenicsmemory.util.withAppLanguage

/**
 * カスタムApplicationクラス
 * ViewModelなどがgetApplication()で取得するContextにも言語設定を適用する
 */
class CalisthenicsMemoryApplication : Application() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(withAppLanguage(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        recordCrashes()
    }

    /**
     * Keeps a report of any crash on the phone (CrashReports), offered for sharing on the next
     * launch. Android's own handler still runs afterwards and closes the app as usual.
     */
    private fun recordCrashes() {
        val reports = CrashReports.forApp(this)
        val context = CrashContext.current()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            reports.record(thread.name, error, context, System.currentTimeMillis())
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    /**
     * Context の言語設定を更新する（全Androidバージョン対応）
     */
}