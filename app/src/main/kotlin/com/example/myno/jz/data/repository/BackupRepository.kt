package com.example.myno.jz.data.repository

import android.content.Context
import com.google.gson.GsonBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(
    private val context: Context
) {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val backupDirectory: File
        get() = File(context.filesDir, "backup")

    /**
     * 创建备份目录
     */
    private fun ensureBackupDirectory() {
        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs()
        }
    }

    /**
     * 创建备份
     *
     * 目前先备份 records.json。
     * 后续分类、设置等 JSON 加入后，
     * 直接扩展这里即可。
     */
    fun createBackup(): File {
        ensureBackupDirectory()

        val time = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val backupFile = File(
            backupDirectory,
            "MoneyBook_Backup_$time.json"
        )

        val recordsFile = File(
            context.filesDir,
            "records.json"
        )

        val recordsJson = if (recordsFile.exists()) {
            recordsFile.readText()
        } else {
            "[]"
        }

        val backupData = linkedMapOf<String, Any>(
            "version" to 1,
            "backupTime" to SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date()),
            "records" to gson.fromJson(
                recordsJson,
                Any::class.java
            )
        )

        backupFile.writeText(
            gson.toJson(backupData)
        )

        return backupFile
    }

    /**
     * 获取所有备份
     */
    fun getBackupFiles(): List<File> {
        ensureBackupDirectory()

        return backupDirectory
            .listFiles()
            ?.filter {
                it.isFile &&
                it.name.startsWith("MoneyBook_Backup_") &&
                it.name.endsWith(".json")
            }
            ?.sortedByDescending {
                it.lastModified()
            }
            ?: emptyList()
    }

    /**
     * 恢复备份
     */
    fun restoreBackup(backupFile: File): Boolean {
        if (!backupFile.exists()) {
            return false
        }

        return try {

            val json = backupFile.readText()

            val root = gson.fromJson(
                json,
                Map::class.java
            )

            val version = (root["version"] as? Number)?.toInt()

            if (version == null) {
                return false
            }

            val records = root["records"]

            val recordsFile = File(
                context.filesDir,
                "records.json"
            )

            recordsFile.writeText(
                gson.toJson(records)
            )

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除备份
     */
    fun deleteBackup(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}