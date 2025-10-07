package com.haodustudio.DailyNotes

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.TypedValue
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.haodustudio.DailyNotes.helper.PrivacySettingsManager
import com.haodustudio.DailyNotes.utils.BitmapUtils
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import rx_activity_result2.RxActivityResult
import java.net.URI
import java.util.*


class BaseApplication : Application(), ViewModelStoreOwner {

    private val appViewModelStore by lazy { ViewModelStore() }
    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    private var sentryInitialized = false

    companion object {
        lateinit var instance: BaseApplication
            private set

        const val APP_ID = "a81252c4145a48a9a52f0d3015a891d9"

        const val ASSETS_MOOD_PATH = "mood/"
        const val ASSETS_TEMPLATE_PATH = "template/"
        const val ASSETS_STICKER_PATH = "sticker/"
        const val ASSETS_DEFAULT_BACKGROUND_PATH = "background/default/"
        const val ASSETS_WEATHER_BACKGROUND_PATH = "background/weather/"
        lateinit var TEMPLATE_DOWNLOAD_FROM_URI_PATH: String
        lateinit var BACKGROUND_DOWNLOAD_FROM_URI_PATH: String
        const val APP_SHARED_PREFERENCES_NAME = "AppConfig"
        const val WEATHER_REFRESH_TIME = 1000*60*60
        const val BASE_SERVER_URI = "example" //已停止服务

    val normalizedBaseServerUrl: String? by lazy { normalizeBaseServerUri(BASE_SERVER_URI) }

        val weatherToPath = mapOf<String, String>(
            "xue" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_snow.webp", //雪
            "lei" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_rain.webp", //雷
            "wu" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_fog.webp",   //雾
            "bingbao" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_snow.webp", //冰雹
            "yun" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_cloudy.webp",  //多云
            "yu" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_rain.webp", //雨
            "yin" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_yin.webp", //阴
            "qing" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_clear_day.webp", //晴
            "shachen" to ASSETS_WEATHER_BACKGROUND_PATH + "bg_dusd_storm.webp" //沙尘
        )

        lateinit var OLD_ASSETS_PATH: String
        lateinit var OLD_DATA_PATH: String
        lateinit var NOTES_PATH: String

        val code2MoodText_old = mapOf<Int,String>(
            1 to "嗨皮的一天～",
            2 to "气死我了，哼！",
            3 to "宝宝不开心！",
            4 to "平凡的一天～",
            5 to "累趴了～",
            6 to "人生巅峰AwA",
            7 to "你爱我，我爱你"
        )

        val idToTextColor = mapOf<Int,Int>(
            1 to R.color.TC_1,
            2 to R.color.TC_2,
            3 to R.color.TC_3,
            4 to R.color.TC_4,
            5 to R.color.TC_5,
            6 to R.color.TC_6,
            7 to R.color.TC_7,
            8 to R.color.TC_8,
            9 to R.color.TC_9,
            10 to R.color.TC_10,
            11 to R.color.TC_11,
            12 to R.color.TC_12
        )

        lateinit var activityBitmap: Bitmap

        private fun normalizeBaseServerUri(raw: String): String? {
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

        fun buildServerUrl(path: String? = null): String? {
            val base = normalizedBaseServerUrl ?: return null
            val trimmedPath = path?.trim()
            if (trimmedPath.isNullOrEmpty()) return base
            val normalizedBase = base.trimEnd('/')
            val normalizedPath = if (trimmedPath.startsWith("/")) trimmedPath else "/$trimmedPath"
            return normalizedBase + normalizedPath
        }
    }

    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(newActivity: Activity, p1: Bundle?) {}
        override fun onActivityStarted(p0: Activity) {}
        override fun onActivityResumed(p0: Activity) {}
        override fun onActivityPaused(p0: Activity) {}
        override fun onActivityStopped(p0: Activity) {}
        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
        override fun onActivityDestroyed(p0: Activity) {}
    }

    override fun onCreate() {
        super.onCreate()
        RxActivityResult.register(this)
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        instance = this
        OLD_DATA_PATH = Environment.getExternalStorageDirectory().path + "/Android/data/" + packageName + '/'
        OLD_ASSETS_PATH = (instance.filesDir?.parent ?: "") + "/assest/" // No spelling mistakes
        NOTES_PATH = filesDir.absolutePath + "/notes/"
        TEMPLATE_DOWNLOAD_FROM_URI_PATH = filesDir.absolutePath + "/uri_template/"
        BACKGROUND_DOWNLOAD_FROM_URI_PATH = filesDir.absolutePath + "/uri_background/"

        updateSentryState(PrivacySettingsManager.isCrashReportingEnabled())
    }

    fun updateSentryState(enabled: Boolean) {
        if (enabled) {
            if (!sentryInitialized) {
                runCatching {
                    SentryAndroid.init(this)
                }.onSuccess {
                    sentryInitialized = true
                }.onFailure {
                    sentryInitialized = false
                }
            }
        } else {
            if (sentryInitialized) {
                Sentry.close()
                sentryInitialized = false
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }
}

val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)