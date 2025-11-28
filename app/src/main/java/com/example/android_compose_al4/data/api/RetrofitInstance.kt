package com.example.android_compose_al4.data.api

import android.content.Context
import com.example.android_compose_al4.R
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitInstance @Inject constructor(
    private val context: Context
) {
    private val gson = GsonBuilder()
        .setLenient()
        .create()
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val mockInterceptor = Interceptor { chain ->
        val url = chain.request().url.toString()

        if (url.contains("my-json-server.typicode.com")) {
            return@Interceptor handleMockResponse(chain)
        }

        try {
            chain.proceed(chain.request())
        } catch (e: Exception) {

            handleMockResponse(chain)
        }
    }
    
    private fun handleMockResponse(chain: Interceptor.Chain): okhttp3.Response {
        val url = chain.request().url.toString()
        val responseCode = 200
        val responseBody = when {
            url.endsWith("db") -> loadJsonFromAssets("mock_data.json")
            url.contains("transactions") -> {
                if (chain.request().method == "GET") {
                    val mockData = loadJsonFromAssets("mock_data.json")
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    val jsonMap = gson.fromJson<Map<String, Any>>(mockData, type)
                    gson.toJson(jsonMap["transactions"])
                } else {
                    // Pour les requêtes POST, on renvoie simplement une réponse de succès
                    "{\"id\": \"new_${System.currentTimeMillis()}\", \"status\": \"success\"}"
                }
            }
            url.contains("profile") -> {
                val mockData = loadJsonFromAssets("mock_data.json")
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val jsonMap = gson.fromJson<Map<String, Any>>(mockData, type)
                gson.toJson(jsonMap["profile"])
            }
            else -> "{}"
        }
        
        return okhttp3.Response.Builder()
            .code(responseCode)
            .message("Mock response")
            .request(chain.request())
            .protocol(Protocol.HTTP_2)
            .body(responseBody.toResponseBody("application/json".toMediaTypeOrNull()))
            .addHeader("content-type", "application/json")
            .build()
    }
    
    private fun loadJsonFromAssets(fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            "{}"
        }
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(mockInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://my-json-server.typicode.com/Maxoubiloux/db-json-android-al4/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: RetrofitInstance? = null
        
        fun getInstance(context: Context): RetrofitInstance {
            return INSTANCE ?: synchronized(this) {
                val instance = RetrofitInstance(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
