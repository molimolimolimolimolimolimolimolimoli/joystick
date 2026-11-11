@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.joyoverlay

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OverlayTheme { Surface(Modifier.fillMaxSize()) { MainScreen() } } }
    }
}

private fun notifGranted(ctx: Context) =
    Build.VERSION.SDK_INT < 33 ||
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun MainScreen() {
    val ctx = LocalContext.current
    var overlayOk by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    var notifOk by remember { mutableStateOf(notifGranted(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayOk = Settings.canDrawOverlays(ctx)
        notifOk = notifGranted(ctx)
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    val running by OverlayService.running.collectAsState()

    var lat by remember { mutableStateOf(Prefs.lat(ctx)) }
    var lon by remember { mutableStateOf(Prefs.lon(ctx)) }
    val latVal = lat.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
    val lonVal = lon.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(ctx.getString(R.string.app_name), style = MaterialTheme.typography.displaySmall)

            StepCard(
                "1. Draw over other apps", "Needed to show the joystick on top of other apps.",
                overlayOk, "Grant",
            ) {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                )
            }
            StepCard(
                "2. Notifications (optional)", "Shows the running notification with a Stop button.",
                notifOk, "Allow",
            ) { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            StepCard(
                "3. Mock location app",
                "Developer options > Select mock location app > choose this app.",
                false, "Open settings",
            ) {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    ctx.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Start position", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            lat, { lat = it }, Modifier.weight(1f), label = { Text("Latitude") },
                            singleLine = true, isError = latVal == null,
                        )
                        OutlinedTextField(
                            lon, { lon = it }, Modifier.weight(1f), label = { Text("Longitude") },
                            singleLine = true, isError = lonVal == null,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (running) {
                        ctx.stopService(Intent(ctx, OverlayService::class.java))
                    } else if (latVal != null && lonVal != null) {
                        Prefs.save(ctx, latVal, lonVal)
                        ContextCompat.startForegroundService(
                            ctx,
                            Intent(ctx, OverlayService::class.java)
                                .putExtra(OverlayService.EXTRA_LAT, latVal)
                                .putExtra(OverlayService.EXTRA_LON, lonVal),
                        )
                    }
                },
                enabled = running || (overlayOk && latVal != null && lonVal != null),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (running) "Stop overlay" else "Start overlay") }
        }
    }
}

@Composable
private fun StepCard(title: String, desc: String, done: Boolean, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodyMedium)
            if (done) Text("Done", style = MaterialTheme.typography.labelLarge)
            else FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}
