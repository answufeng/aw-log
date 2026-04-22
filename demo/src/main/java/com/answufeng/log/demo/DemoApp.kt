package com.answufeng.log.demo

import android.app.Application

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Demo 初始化交由 MainActivity 的「应用配置」控制，便于演示不同组合。
    }
}
