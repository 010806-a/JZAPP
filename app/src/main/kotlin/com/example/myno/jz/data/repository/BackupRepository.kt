package com.example.myno.jz.data.repository

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MoneyBook 本地数据备份仓库。
 *
 * 备份范围与 JsonDataStore 保持一致：
 * records / accounts / categories / budgets / transfers / settings。
 */
class BackupRepository(
    private val context: Context
) {

    companion object {
        private const val BACKUP_VERSION = 2
        private const val BACKUP_PREFIX = "MoneyBook_Backup_"
        private const val RECORDS_FILE = "records.json"
        private const val ACCOUNTS_FILE = "accounts.json"
        private const val CATEGORIES_FILE = "categories.json"
        private const val BUDGETS_FILE = "budgets.json"
        private const val TRANSFERS_FILE = "transfers.json"
        private const val SETTINGS_FILE = "settings.json"
    }

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val backupDirectory: File
        get() = File(context.filesDir, "backup")

    private fun ensureBackupDirectory() {
        if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
            throw IllegalStateException("无法创建备份目录")
        }
    }

    private fun readJsonOrDefault(fileName: String, defaultJson: String): JsonElement {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return gson.fromJson(defaultJson, JsonElement::class.java)

        val text = file.readText(Charsets.UTF_8)
        if (text.isBlank()) return gson.fromJson(defaultJson, JsonElement::class.java)

        return gson.fromJson(text, JsonElement::class.java)
            ?: gson.fromJson(defaultJson, JsonElement::class.java)
    }

    /** 创建完整本地备份。 */
    fun createBackup(): File {
        ensureBackupDirectory()

        val now = Date()
        val fileTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(now)
        val displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(now)

        val backupFile = File(backupDirectory, "${BACKUP_PREFIX}${fileTime}.json")
        val root = JsonObject().apply {
            addProperty("version", BACKUP_VERSION)
            addProperty("backupTime", displayTime)
            add("records", readJsonOrDefault(RECORDS_FILE, "[]"))
            add("accounts", readJsonOrDefault(ACCOUNTS_FILE, "[]"))
            add("categories", readJsonOrDefault(CATEGORIES_FILE, "[]"))
            add("budgets", readJsonOrDefault(BUDGETS_FILE, "[]"))
            add("transfers", readJsonOrDefault(TRANSFERS_FILE, "[]"))
            add("settings", readJsonOrDefault(SETTINGS_FILE, "{}"))
        }

        backupFile.writeText(gson.toJson(root), Charsets.UTF_8)

        if (!backupFile.exists() || backupFile.length() == 0L) {
            throw IllegalStateException("备份文件写入失败")
        }

        return backupFile
    }

    fun getBackupFiles(): List<File> {
        ensureBackupDirectory()

        return backupDirectory
            .listFiles()
            ?.filter {
                it.isFile &&
                    it.name.startsWith(BACKUP_PREFIX) &&
                    it.name.endsWith(".json")
            }
            ?.sortedByDescending(File::lastModified)
            ?: emptyList()
    }

    /**
     * 恢复完整备份。
     * 只有确认所有目标文件均可成功写入后才返回 true。
     */
    fun restoreBackup(backupFile: File): Boolean {
        if (!backupFile.exists() || !backupFile.isFile) return false

        return try {
            val root = gson.fromJson(backupFile.readText(Charsets.UTF_8), JsonObject::class.java)
                ?: return false

            val version = root.get("version")?.asInt ?: return false
            if (version !in 1..BACKUP_VERSION) return false

            // 旧版 v1 只包含 records，仍允许恢复，其他数据保持当前状态。
            val filesToRestore = linkedMapOf(
                RECORDS_FILE to root.get("records"),
                ACCOUNTS_FILE to root.get("accounts"),
                CATEGORIES_FILE to root.get("categories"),
                BUDGETS_FILE to root.get("budgets"),
                TRANSFERS_FILE to root.get("transfers"),
                SETTINGS_FILE to root.get("settings")
            )

            filesToRestore.forEach { (fileName, element) ->
                if (element != null && !element.isJsonNull) {
                    val target = File(context.filesDir, fileName)
                    val temp = File(context.filesDir, "$fileName.restore.tmp")
                    temp.writeText(gson.toJson(element), Charsets.UTF_8)
                    if (!temp.exists() || temp.length() == 0L) {
                        temp.delete()
                        throw IllegalStateException("恢复 $fileName 失败")
                    }
                    if (target.exists() && !target.delete()) {
                        temp.delete()
                        throw IllegalStateException("无法替换 $fileName")
                    }
                    if (!temp.renameTo(target)) {
                        temp.delete()
                        throw IllegalStateException("无法写入 $fileName")
                    }
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteBackup(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}
