package com.example.android_compose_al4.data.auth

import android.content.Context
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

data class UserCredential(
    val email: String,
    val password: String
)

data class MockAuthData(
    val users: List<UserCredential>? = null
)

object AuthProvider {
    private const val FILE_NAME = "mock_data.json"

    private fun loadAuthData(context: Context): MockAuthData? {
        return try {
            context.assets.open(FILE_NAME).use { input ->
                val reader = BufferedReader(InputStreamReader(input))
                val content = reader.readText()
                Gson().fromJson(content, MockAuthData::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun validate(context: Context, email: String, password: String): Boolean {
        val data = loadAuthData(context)
        val list = data?.users ?: emptyList()
        return list.any { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }
    }
}
