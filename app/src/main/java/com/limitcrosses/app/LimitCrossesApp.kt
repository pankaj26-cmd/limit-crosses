package com.limitcrosses.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.limitcrosses.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LimitCrossesApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@LimitCrossesApp)
            modules(appModule)
        }

        // Initialize exact midnight reset alarm sequence
        com.limitcrosses.app.platform.receiver.MidnightResetReceiver.scheduleNextMidnightAlarm(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                try {
                    com.limitcrosses.app.platform.service.AppMonitorBlockerService.start(activity)
                } catch (_: Exception) {}
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}

