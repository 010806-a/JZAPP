package com.example.myno.jz.ui.privacy

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import com.example.myno.jz.data.repository.PrivacyLockStore

object PasswordSetupHelper {

    fun save(
        context: Context,
        lockStore: PrivacyLockStore,
        passwordInput: EditText,
        confirmInput: EditText,
        successMessage: String,
        failureMessage: String,
        onSuccess: () -> Unit
    ) {
        val password =
            passwordInput.text
                ?.toString()
                .orEmpty()

        val confirmPassword =
            confirmInput.text
                ?.toString()
                .orEmpty()

        if (password.length < 6) {
            passwordInput.error =
                "密码至少需要6位"
            return
        }

        if (password != confirmPassword) {
            confirmInput.error =
                "两次输入的密码不一致"
            return
        }

        val success =
            lockStore.savePassword(password)

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