package com.example.myno.jz.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val TAG = "MoneyBook"

    private const val LOG_DIRECTORY = "logs"
    private const val LOG_FILE_NAME = "moneybook.log"

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.CHINA
        )

    private var initialized = false

    private var logFile: File? = null

    /**
     * 初始化日志系统
     */
    fun init(context: Context) {

        try {

            val appContext =
                context.applicationContext

            val directory =
                File(
                    appContext.getExternalFilesDir(null),
                    LOG_DIRECTORY
                )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            logFile =
                File(
                    directory,
                    LOG_FILE_NAME
                )

            initialized = true

            i(
                "AppLogger",
                "日志系统初始化成功"
            )

            i(
                "AppLogger",
                "日志文件：${logFile?.absolutePath}"
            )

        } catch (e: Exception) {

            initialized = false

            Log.e(
                TAG,
                "日志系统初始化失败",
                e
            )
        }
    }

    /**
     * 普通信息
     */
    fun i(
        tag: String,
        message: String
    ) {

        write(
            "INFO",
            tag,
            message
        )
    }

    /**
     * 警告
     */
    fun w(
        tag: String,
        message: String
    ) {

        write(
            "WARN",
            tag,
            message
        )
    }

    /**
     * 错误
     */
    fun e(
        tag: String,
        message: String
    ) {

        write(
            "ERROR",
            tag,
            message
        )
    }

    /**
     * 错误 + 异常
     */
    fun e(
        tag: String,
        message: String,
        throwable: Throwable
    ) {

        write(
            "ERROR",
            tag,
            "$message\n${Log.getStackTraceString(throwable)}"
        )
    }
fun crash(
    tag: String,
    message: String
) {
    write(
        "CRASH",
        tag,
        message
    )
}
    /**
     * 写入日志
     */
    private fun write(
        level: String,
        tag: String,
        message: String
    ) {

        val time =
            dateFormat.format(
                Date()
            )

        val logMessage =
            "[$time] [$level] [$tag] $message"

when (level) {

    "INFO" ->
        Log.i(
            "$TAG-$tag",
            message
        )

    "WARN" ->
        Log.w(
            "$TAG-$tag",
            message
        )

    "ERROR" ->
        Log.e(
            "$TAG-$tag",
            message
        )

    "CRASH" ->
        Log.e(
            "$TAG-$tag",
            message
        )
}

        if (!initialized) {
            return
        }

        try {

            val file =
                logFile ?: return

            file.appendText(
                "$logMessage\n",
                Charsets.UTF_8
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "写入日志文件失败",
                e
            )
        }
    }

    /**
     * 获取日志文件
     */
    fun getLogFile(): File? {
        return logFile
    }

    /**
     * 获取全部日志
     */
    fun readLogs(): String {

        return try {

            val file =
                logFile

            if (
                file == null ||
                !file.exists()
            ) {
                ""
            } else {
                file.readText(
                    Charsets.UTF_8
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "读取日志失败",
                e
            )

            ""
        }
    }

    /**
     * 清空日志
     */
    fun clearLogs(): Boolean {

        return try {

            val file =
                logFile

            if (
                file != null &&
                file.exists()
            ) {
                file.writeText(
                    "",
                    Charsets.UTF_8
                )
            }

            true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "清空日志失败",
                e
            )

            false
        }
    }
}