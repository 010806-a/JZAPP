package com.example.myno.jz

import android.app.Application
import com.example.myno.jz.utils.AppLogger
import com.example.myno.jz.utils.CrashHandler

class MoneyBookApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化应用日志
        AppLogger.init(this)

        // 安装全局崩溃捕获
        CrashHandler.install(this)
    }
}