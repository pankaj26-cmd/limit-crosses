package com.limitcrosses.app

import android.app.Application
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
    }
}

