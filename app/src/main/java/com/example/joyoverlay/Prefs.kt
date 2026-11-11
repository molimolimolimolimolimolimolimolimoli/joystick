package com.example.joyoverlay

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("joy", Context.MODE_PRIVATE)
    fun lat(c: Context): String = sp(c).getString("lat", "48.8584")!!
    fun lon(c: Context): String = sp(c).getString("lon", "2.2945")!!
    fun save(c: Context, lat: Double, lon: Double) {
        sp(c).edit().putString("lat", lat.toString()).putString("lon", lon.toString()).apply()
    }
}
