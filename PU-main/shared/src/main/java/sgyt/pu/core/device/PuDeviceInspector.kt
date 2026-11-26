package sgyt.pu.core.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import sgyt.pu.core.model.PuDeviceSnapshot
import sgyt.pu.core.model.PuEditionRecommendation
import sgyt.pu.core.model.PuDeviceTier
import sgyt.pu.model.PuEdition
import sgyt.pu.model.PuProfile

/** Collects the device facts required by PU's launch gate and tier policy. */
class PuDeviceInspector(private val context: Context) : PuDeviceProbe {
    override fun snapshot(): PuDeviceSnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val storage = StatFs(context.filesDir.path)
        val battery = readBattery()
        val (glMajor, glMinor) = readGlVersion()
        val webView = readWebViewVersion()

        return PuDeviceSnapshot(
            capturedAt = System.currentTimeMillis(),
            apiLevel = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            totalRamBytes = memoryInfo.totalMem,
            availableRamBytes = memoryInfo.availMem,
            appMemoryClassMb = activityManager.memoryClass,
            freeStorageBytes = storage.availableBytes,
            totalStorageBytes = storage.totalBytes,
            glEsMajor = glMajor,
            glEsMinor = glMinor,
            webViewVersionName = webView?.first,
            webViewVersionCode = webView?.second ?: 0,
            isCharging = battery.second,
            batteryPercent = battery.first,
            thermalStatus = readThermalStatus(),
        )
    }

    override fun recommend(snapshot: PuDeviceSnapshot): PuEditionRecommendation = when (snapshot.tier) {
        PuDeviceTier.NATIVE -> PuEditionRecommendation(
            edition = PuEdition.LITE,
            reason = "This device is best suited to PU Lite's low-memory profile.",
            profile = PuProfile.ULTRA_LOW,
        )

        PuDeviceTier.WINE_LITE -> PuEditionRecommendation(
            edition = PuEdition.LITE,
            reason = "This device has about 3 GB of memory. PU Lite is the recommended edition.",
            profile = PuProfile.LOW,
        )

        PuDeviceTier.WINE_FULL -> PuEditionRecommendation(
            edition = PuEdition.PLUS,
            reason = "This device has 4 GB or more of memory. PU Plus can use the balanced profile and Plus features.",
            profile = PuProfile.BALANCED,
        )
    }

    private fun readBattery(): Pair<Int, Boolean> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 0 to false
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val percent = ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return percent to charging
    }

    private fun readThermalStatus(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return runCatching { powerManager.currentThermalStatus }.getOrNull()
    }

    private fun readWebViewVersion(): Pair<String?, Long>? {
        val packageManager = context.packageManager
        val providers = linkedSetOf(
            "com.google.android.webview",
            "com.android.webview",
            "com.google.android.trichromelibrary",
            "com.sec.android.webview",
            "org.chromium.webview_shell",
        )
        providers.forEach { provider ->
            runCatching {
                val info = packageManager.getPackageInfo(provider, 0)
                info.versionName to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
            }.getOrNull()?.let { return it }
        }

        val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.invalid"))
        val resolved = packageManager.queryIntentActivities(webIntent, PackageManager.MATCH_ALL)
            .firstOrNull { it.activityInfo?.packageName?.contains("webview", ignoreCase = true) == true }
            ?: return null
        val info = packageManager.getPackageInfo(resolved.activityInfo.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return info.versionName to versionCode
    }

    /**
     * Checks the real EGL implementation, not only the manifest requirement.
     * The ES3 renderable bit is part of EGL_KHR_create_context and is present
     * on Android implementations that expose OpenGL ES 3.0.
     */
    private fun readGlVersion(): Pair<Int, Int> {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return 2 to 0
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return 2 to 0

        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        try {
            val configAttributes = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            if (!EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, 1, configCount, 0) ||
                configCount[0] == 0
            ) {
                return 2 to 0
            }

            val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) return 2 to 0

            val surfaceAttributes = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttributes, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) return 2 to 0
            if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) return 2 to 0

            val value = IntArray(1)
            EGL14.eglGetIntegerv(EGL14.EGL_VERSION, value, 0)
            val major = value[0].let { encoded ->
                if (encoded >= 0x00030000) 3 else 2
            }
            val versionText = EGL14.eglQueryString(display, EGL14.EGL_VERSION).orEmpty()
            val minor = Regex("OpenGL ES (\\d+\\.\\d+)").find(versionText)
                ?.groupValues?.getOrNull(1)
                ?.substringAfter('.')
                ?.toIntOrNull() ?: 0
            return major to minor
        } finally {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)
        }
    }

    private companion object {
        const val EGL_OPENGL_ES3_BIT = 0x00000040
    }
}
