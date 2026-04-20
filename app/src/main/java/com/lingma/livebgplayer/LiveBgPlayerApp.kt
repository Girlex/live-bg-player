package com.lingma.livebgplayer

import android.app.Application
import timber.log.Timber

class LiveBgPlayerApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
