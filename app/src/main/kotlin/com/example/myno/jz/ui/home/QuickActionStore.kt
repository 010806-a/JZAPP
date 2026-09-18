package com.example.myno.jz.ui.home

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuickActionStore(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val gson =
        Gson()

    private val fileName =
        "quick_actions.json"

    fun load(): List<QuickAction> {

        return try {

            val file =
                appContext.getFileStreamPath(
                    fileName
                )

            if (!file.exists()) {
                return emptyList()
            }

            val json =
                appContext.openFileInput(
                    fileName
                ).bufferedReader().use {
                    it.readText()
                }

            if (json.isBlank()) {
                emptyList()
            } else {

                val type =
                    object : TypeToken<List<QuickAction>>() {}.type

                gson.fromJson<List<QuickAction>>(
                    json,
                    type
                ) ?: emptyList()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }

    fun save(
        actions: List<QuickAction>
    ) {

        try {

            val json =
                gson.toJson(actions)

            appContext.openFileOutput(
                fileName,
                Context.MODE_PRIVATE
            ).use { output ->

                output.write(
                    json.toByteArray(
                        Charsets.UTF_8
                    )
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}