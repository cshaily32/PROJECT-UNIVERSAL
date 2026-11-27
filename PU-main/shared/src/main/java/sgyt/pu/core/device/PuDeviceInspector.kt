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
import android.opengl.GLES20
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import java.util.concurrent.atomic.AtomicReference
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
     * Resolves the device's OpenGL ES version from the platform itself, never from a
     * hardcoded table, and never calls a GL entry point outside a current EGL context.
     *
     * Calling [GLES20.glGetString] without a current context is what killed the app with a
     * native SIGSEGV inside `libGLESv1_CM.so` on some devices, so the raw query now runs
     * against a throw-away offscreen context.
     *
     * Two sources are combined:
     * 1. [ActivityManager.getDeviceConfigurationInfo] - the system server reports the device
     *    `ro.opengles.version` here. Pure Binder/Java, no native GL involved at all.
     * 2. An offscreen EGL pbuffer context queried through `glGetString(GL_VERSION)`, which
     *    yields the exact driver version (for example 3.2 instead of 3.0).
     *
     * The probe can only refine the answer, never lower it: a driver exposing less than the
     * device declares must not hide capability the platform reports.
     */
    private fun readGlVersion(): Pair<Int, Int> {
        val declared = declaredGlEsVersion()
        val probed = probeGlEsVersion()
        return when {
            probed == null -> declared
            declared.first > probed.first -> declared
            else -> probed
        }
    }

    /**
     * Device GL ES version reported by the platform.
     *
     * `ActivityManager.getDeviceConfigurationInfo()` exposes `ro.opengles.version`, encoded
     * with the major version in the upper 16 bits and the minor version in the lower 16 bits.
     */
    private fun declaredGlEsVersion(): Pair<Int, Int> {
        val encoded = runCatching {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.deviceConfigurationInfo?.reqGlEsVersion
        }.getOrNull() ?: return MINIMUM_SUPPORTED_GL_ES
        val major = (encoded shr 16) and 0xFFFF
        val minor = encoded and 0xFFFF
        return if (major < MINIMUM_SUPPORTED_GL_ES.first) MINIMUM_SUPPORTED_GL_ES else major to minor
    }

    /**
     * Reads the exact driver version from a throw-away offscreen EGL context.
     *
     * A GL context is bound to the thread that made it current, so the whole probe
     * (create -> make current -> query -> destroy) runs on one dedicated thread and never
     * touches the caller's thread, which may be the main thread.
     */
    private fun probeGlEsVersion(): Pair<Int, Int>? {
        val probed = AtomicReference<Pair<Int, Int>?>(null)
        val worker = Thread(
            { probed.set(runCatching { readGlVersionWithEglContext() }.getOrNull()) },
            "pu-gl-probe",
        ).apply { isDaemon = true }
        worker.start()
        worker.join(GL_PROBE_TIMEOUT_MS)
        return probed.get()
    }

    private fun readGlVersionWithEglContext(): Pair<Int, Int>? {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return null
        val versions = IntArray(2)
        if (!EGL14.eglInitialize(display, versions, 0, versions, 1)) return null
        // The display stays initialized on purpose: terminating it would tear the default
        // display down for any other EGL user in this process.
        EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API)

        val config = chooseEglConfig(display) ?: return null
        val context = createEglContext(display, config) ?: return null
        val surface = EGL14.eglCreatePbufferSurface(display, config, PBUFFER_ATTRIBUTES, 0)
        if (surface == EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroyContext(display, context)
            return null
        }
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
            return null
        }
        try {
            val versionText = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
            val match = GL_VERSION_PATTERN.find(versionText) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            return major to minor
        } finally {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
        }
    }

    /**
     * Picks the richest config the driver offers: an ES 3 capable one first so the probe can
     * report the real minor version, falling back to plain ES 2.
     */
    private fun chooseEglConfig(display: EGLDisplay): EGLConfig? {
        val renderableTypes = intArrayOf(EGL_OPENGL_ES3_BIT, EGL14.EGL_OPENGL_ES2_BIT)
        renderableTypes.forEach { renderableType ->
            val attributes = intArrayOf(
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            val chosen = EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, configCount, 0)
            if (chosen && configCount[0] > 0) {
                configs[0]?.let { return it }
            }
        }
        return null
    }

    /**
     * Creates a context for the newest GL ES version the driver accepts. A refusal is
     * reported as `EGL_NO_CONTEXT`, never as a crash, so the next client version is tried.
     */
    private fun createEglContext(display: EGLDisplay, config: EGLConfig): EGLContext? {
        val clientVersions = intArrayOf(3, 2)
        clientVersions.forEach { clientVersion ->
            val attributes = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion,
                EGL14.EGL_NONE,
            )
            val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attributes, 0)
            if (context != EGL14.EGL_NO_CONTEXT) return context
        }
        return null
    }

    private companion object {
        /** EGL_OPENGL_ES3_BIT_KHR, not exposed by the android.opengl.EGL14 wrapper. */
        const val EGL_OPENGL_ES3_BIT = 0x00000040

        /** Android guarantees OpenGL ES 2.0 on every device from API 18 onwards (this app is API 24+). */
        val MINIMUM_SUPPORTED_GL_ES = 2 to 0

        const val GL_PROBE_TIMEOUT_MS = 1_500L

        val GL_VERSION_PATTERN = Regex("""OpenGL ES (\d+)\.(\d+)""")

        val PBUFFER_ATTRIBUTES = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE,
        )
    }
}
