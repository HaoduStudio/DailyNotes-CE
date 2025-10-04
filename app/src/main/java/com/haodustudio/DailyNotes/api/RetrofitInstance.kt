package com.haodustudio.DailyNotes.api

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit


class RetrofitInstance {

    companion object {
        private const val TAG = "RetrofitInstance"
        @Volatile
        private var instance: Retrofit? = null
        @Volatile
        private var cachedBaseUrl: String? = null

        fun getInstance(baseUri: String): Retrofit? {
            val normalized = normalizeBaseUrl(baseUri) ?: run {
                Log.w(TAG, "Invalid base url: $baseUri")
                return null
            }

            val cached = instance
            if (cached != null && cachedBaseUrl == normalized) {
                return cached
            }

            return Retrofit.Builder()
                .client(getOkHttpClient())
                .baseUrl(normalized)
                .addConverterFactory(GsonConverterFactory.create())
                .build().also {
                    instance = it
                    cachedBaseUrl = normalized
                }
        }

        private fun normalizeBaseUrl(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
            val withTrailingSlash = if (withScheme.endsWith("/")) withScheme else "$withScheme/"
            return try {
                val uri = URI(withTrailingSlash)
                val scheme = uri.scheme?.lowercase(Locale.getDefault())
                if (scheme == "http" || scheme == "https") withTrailingSlash else null
            } catch (_: Exception) {
                null
            }
        }

        private fun getOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request()
                        .newBuilder()
                        .removeHeader("User-Agent") //移除旧的
                        .addHeader("User-Agent", getUserAgent()) //添加真正的头部
                        .build()
                    chain.proceed(request)
                }.build()
        }

        private fun getUserAgent(): String {
            val sb = StringBuffer()
            val userAgent = System.getProperty("http.agent") as String
            var i = 0
            val length = userAgent.length
            while (i < length) {
                val c = userAgent[i]
                if (c <= '\u001f' || c >= '\u007f') {
                    sb.append(String.format("\\u%04x", c.code))
                } else {
                    sb.append(c)
                }
                i++
            }
            Log.v("User-Agent", "User-Agent: $sb")
            return sb.toString()
        }
    }
}
