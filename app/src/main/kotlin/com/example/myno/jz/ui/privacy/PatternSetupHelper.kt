package com.example.myno.jz.ui.privacy

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import com.example.myno.jz.data.repository.PrivacyLockStore

class PatternSetupHelper(
    private val context: Context,
    private val lockStore: PrivacyLockStore,
    private val statusView: TextView,
    private val resetPattern: () -> Unit,
    private val successMessage: String,
    private val failureMessage: String,
    private val onSuccess: () -> Unit
) {

    private var firstPattern: String? = null
    private var waitingForSecondPattern = false

    fun handle(pattern: String) {

        val pointCount =
            pattern.split("-").size

        if (pointCount < 4) {

            statusView.text =
                "图案至少需要连接4个点"

            resetPattern()

            return
        }

        if (!waitingForSecondPattern) {

            firstPattern = pattern
            waitingForSecondPattern = true

            statusView.text =
                "图案已记录，请再次绘制相同图案确认"

            resetPattern()

            return
        }

        if (pattern != firstPattern) {

            firstPattern = null
            waitingForSecondPattern = false

            statusView.text =
                "两次图案不一致，请重新设置"

            resetPattern()

            return
        }

        save(pattern)
    }

    private fun save(pattern: String) {

        val success =
            lockStore.savePattern(pattern)

        if (!success) {

            Toast.makeText(
                context,
                failureMessage,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            context,
            successMessage,
            Toast.LENGTH_SHORT
        ).show()

        onSuccess()
    }
}