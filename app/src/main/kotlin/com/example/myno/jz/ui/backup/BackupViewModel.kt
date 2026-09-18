package com.example.myno.jz.ui.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myno.jz.data.repository.BackupRepository
import java.io.File

class BackupViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        BackupRepository(application.applicationContext)

    private val _backupFiles =
        MutableLiveData<List<File>>()

    val backupFiles: LiveData<List<File>>
        get() = _backupFiles

    private val _message =
        MutableLiveData<String>()

    val message: LiveData<String>
        get() = _message

    fun loadBackups() {
        _backupFiles.value =
            repository.getBackupFiles()
    }

    fun createBackup() {
        try {
            val file = repository.createBackup()

            _message.value =
                "完整备份成功：${file.name}"

            loadBackups()

        } catch (e: Exception) {

            e.printStackTrace()

            _message.value =
                "备份失败：${e.message}"
        }
    }

    fun restoreBackup(file: File) {

        val success =
            repository.restoreBackup(file)

        if (success) {

            _message.value =
                "完整数据恢复成功，请重新进入首页查看数据"

        } else {

            _message.value =
                "恢复失败，备份文件可能已经损坏"
        }
    }

    fun deleteBackup(file: File) {

        if (repository.deleteBackup(file)) {

            _message.value =
                "备份已删除"

            loadBackups()

        } else {

            _message.value =
                "删除失败"
        }
    }
}