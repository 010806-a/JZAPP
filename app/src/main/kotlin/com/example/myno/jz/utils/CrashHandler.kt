package com.example.myno.jz.utils

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashHandler {

    private var initialized = false

    private var defaultHandler:
        Thread.UncaughtExceptionHandler? = null

    private var appContext: Context? = null

    fun install(context: Context) {

        if (initialized) {
            return
        }

        try {

            appContext =
                context.applicationContext

            defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler()

            Thread.setDefaultUncaughtExceptionHandler(
                object : Thread.UncaughtExceptionHandler {

                    override fun uncaughtException(
                        thread: Thread,
                        throwable: Throwable
                    ) {

                        handleCrash(
                            thread,
                            throwable
                        )

                        // 交给 Android 原本的异常处理机制
                        defaultHandler?.uncaughtException(
                            thread,
                            throwable
                        ) ?: run {

                            Process.killProcess(
                                Process.myPid()
                            )

                            exitProcess(10)
                        }
                    }
                }
            )

            initialized = true

            AppLogger.i(
                "CrashHandler",
                "全局崩溃捕获器安装成功"
            )

        } catch (e: Exception) {

            AppLogger.e(
                "CrashHandler",
                "安装全局崩溃捕获器失败",
                e
            )
        }
    }

    private fun handleCrash(
        thread: Thread,
        throwable: Throwable
    ) {

        try {

            val context =
                appContext

            if (context == null) {

                AppLogger.e(
                    "CrashHandler",
                    "Application Context 为空，无法获取应用信息"
                )
            }

            val stackTrace =
                StringWriter()

            throwable.printStackTrace(
                PrintWriter(stackTrace)
            )

            val packageName =
                context?.packageName
                    ?: "未知"

            val versionName =
                getVersionName(context)

            val versionCode =
                getVersionCode(context)

            val manufacturer =
                Build.MANUFACTURER

            val model =
                Build.MODEL

            val androidVersion =
                Build.VERSION.RELEASE

            val apiLevel =
                Build.VERSION.SDK_INT

            val message =
                buildString {

                    appendLine(
                        "========== APP CRASH =========="
                    )

                    appendLine(
                        "应用：MoneyBook"
                    )

                    appendLine(
                        "包名：$packageName"
                    )

                    appendLine(
                        "版本：$versionName ($versionCode)"
                    )

                    appendLine(
                        "Android：$androidVersion"
                    )

                    appendLine(
                        "API：$apiLevel"
                    )

                    appendLine(
                        "设备：$manufacturer $model"
                    )

                    appendLine(
                        "线程：${thread.name}"
                    )

                    appendLine(
                        "异常类型：${throwable.javaClass.name}"
                    )

                    appendLine(
                        "异常信息：${throwable.message}"
                    )

                    appendLine()

                    appendLine(
                        "完整堆栈："
                    )

                    appendLine(
                        stackTrace.toString()
                    )

                    appendLine(
                        "========== APP CRASH END =========="
                    )
                }

            AppLogger.crash(
                "CrashHandler",
                message
            )

        } catch (e: Exception) {

            // 崩溃处理本身不能再次导致程序异常
            AppLogger.e(
                "CrashHandler",
                "处理崩溃信息时发生异常",
                e
            )
        }
    }

    private fun getVersionName(
        context: Context?
    ): String {

        return try {

            if (context == null) {
                return "未知"
            }

            @Suppress("DEPRECATION")
            context.packageManager
                .getPackageInfo(
                    context.packageName,
                    0
                )
                .versionName
                ?: "未知"

        } catch (e: Exception) {

            "未知"
        }
    }

    private fun getVersionCode(
        context: Context?
    ): Long {

        return try {

            if (context == null) {
                return -1L
            }

            @Suppress("DEPRECATION")
            val packageInfo =
                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        0
                    )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                packageInfo.longVersionCode

            } else {

                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

        } catch (e: Exception) {

            -1L
        }
    }
}