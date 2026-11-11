package com.example.joyoverlay

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Moves a position based on joystick input and publishes it as a mock location every second. */
class MockEngine(private val context: Context, startLat: Double, startLon: Double) {

    data class State(
        val lat: Double,
        val lon: Double,
        val bearing: Float = 0f,
        val speedSetting: Float = 1.4f, // m/s at full joystick deflection
        val currentSpeed: Float = 0f,
        val paused: Boolean = false,
        val error: String? = null,
    )

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    private val _state = MutableStateFlow(State(startLat, startLon))
    val state: StateFlow<State> = _state

    @Volatile private var jx = 0f
    @Volatile private var jy = 0f
    private var job: Job? = null

    fun setJoystick(x: Float, y: Float) { jx = x; jy = y }
    fun setSpeed(v: Float) = _state.update { it.copy(speedSetting = v) }
    fun setPaused(p: Boolean) = _state.update { it.copy(paused = p) }

    fun start(scope: CoroutineScope) {
        setupProviders()
        job = scope.launch(Dispatchers.Default) {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                val dt = (now - last) / 1000.0
                last = now
                val s = _state.updateAndGet { cur ->
                    val mag = min(1f, hypot(jx, jy))
                    if (cur.paused || mag < 0.08f) {
                        cur.copy(currentSpeed = 0f)
                    } else {
                        val bearing = ((Math.toDegrees(atan2(jx.toDouble(), -jy.toDouble())) + 360.0) % 360.0)
                        val speed = cur.speedSetting * mag
                        val d = speed * dt
                        val rad = Math.toRadians(bearing)
                        val lat = (cur.lat + d * cos(rad) / 111_320.0).coerceIn(-85.0, 85.0)
                        var lon = cur.lon + d * sin(rad) / (111_320.0 * cos(Math.toRadians(lat)))
                        if (lon > 180) lon -= 360 else if (lon < -180) lon += 360
                        cur.copy(lat = lat, lon = lon, bearing = bearing.toFloat(), currentSpeed = speed)
                    }
                }
                push(s)
            }
        }
    }

    fun stop() {
        job?.cancel()
        providers.forEach {
            try { lm.setTestProviderEnabled(it, false) } catch (_: Exception) {}
            try { lm.removeTestProvider(it) } catch (_: Exception) {}
        }
        val s = _state.value
        Prefs.save(context, s.lat, s.lon)
    }

    @Suppress("DEPRECATION")
    private fun setupProviders() {
        try {
            providers.forEach {
                try { lm.removeTestProvider(it) } catch (_: Exception) {}
                lm.addTestProvider(
                    it, false, false, false, false, true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE,
                )
                lm.setTestProviderEnabled(it, true)
            }
        } catch (e: SecurityException) {
            _state.update { it.copy(error = "Pick this app as the mock location app in Developer options.") }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message ?: "Could not start mock provider.") }
        }
    }

    private fun push(s: State) {
        providers.forEach { p ->
            try {
                val loc = Location(p).apply {
                    latitude = s.lat
                    longitude = s.lon
                    accuracy = 3f
                    altitude = 0.0
                    bearing = s.bearing
                    speed = s.currentSpeed
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
                lm.setTestProviderLocation(p, loc)
            } catch (e: SecurityException) {
                _state.update { it.copy(error = "Pick this app as the mock location app in Developer options.") }
            } catch (_: Exception) {
            }
        }
    }
}
