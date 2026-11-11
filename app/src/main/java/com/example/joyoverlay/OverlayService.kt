package com.example.joyoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

class OverlayService : Service() {

    companion object {
        const val ACTION_STOP = "com.example.joyoverlay.STOP"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        private const val CHANNEL = "overlay"
        val running = MutableStateFlow(false)
    }

    private lateinit var wm: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null
    private var engine: MockEngine? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        running.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (view == null) {
            if (!Settings.canDrawOverlays(this)) {
                stopSelf()
                return START_NOT_STICKY
            }
            showOverlay(intent?.getDoubleExtra(EXTRA_LAT, 0.0) ?: 0.0, intent?.getDoubleExtra(EXTRA_LON, 0.0) ?: 0.0)
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(lat: Double, lon: Double) {
        val lifecycleOwner = OverlayLifecycleOwner().also { it.create() }
        owner = lifecycleOwner
        val eng = MockEngine(this, lat, lon).also { it.start(scope) }
        engine = eng

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        val composeView = ComposeView(this)
        ViewTreeLifecycleOwner.set(composeView, lifecycleOwner)
        ViewTreeSavedStateRegistryOwner.set(composeView, lifecycleOwner)
        composeView.setContent {
            OverlayTheme {
                OverlayContent(
                    engine = eng,
                    onDrag = { dx, dy ->
                        lp.x += dx.roundToInt()
                        lp.y += dy.roundToInt()
                        wm.updateViewLayout(composeView, lp)
                    },
                    onClose = { stopSelf() },
                )
            }
        }
        wm.addView(composeView, lp)
        view = composeView
        lifecycleOwner.resume()
    }

    private fun promoteToForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Overlay", NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Overlay is running")
            .setOngoing(true)
            .addAction(0, "Stop", stop)
            .build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    override fun onDestroy() {
        running.value = false
        engine?.stop()
        view?.let {
            it.disposeComposition()
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        owner?.destroy()
        scope.cancel()
        super.onDestroy()
    }
}
