package com.answufeng.log.demo

import android.app.Application
import com.answufeng.log.AwDesensitizeInterceptor
import com.answufeng.log.AwLogger

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AwLogger.init {
            debug = true
            fileLog = true
            fileDir = cacheDir.absolutePath + "/logs"
            maxFileSize = 2L * 1024 * 1024
            maxFileCount = 5
            crashLog = true
            addInterceptor(AwDesensitizeInterceptor.create {
                phone()
                email()
                keyValue()
            })
        }
    }
}
