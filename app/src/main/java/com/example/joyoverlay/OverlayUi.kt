@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)

package com.example.joyoverlay

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val presets = listOf("Walk" to 1.4f, "Run" to 3.0f, "Bike" to 6.0f, "Car" to 14.0f)

@Composable
fun OverlayContent(engine: MockEngine, onDrag: (Float, Float) -> Unit, onClose: () -> Unit) {
    val state by engine.state.collectAsState()
    var expanded by remember { mutableStateOf(true) }
    val ms = MaterialTheme.motionScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TopPill(expanded = expanded, onToggle = { expanded = !expanded }, onDrag = onDrag, onClose = onClose)

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(ms.defaultEffectsSpec()) +
                expandVertically(ms.defaultSpatialSpec<IntSize>()) +
                scaleIn(ms.defaultSpatialSpec(), initialScale = 0.85f, transformOrigin = TransformOrigin(0f, 0f)),
            exit = fadeOut(ms.defaultEffectsSpec()) +
                shrinkVertically(ms.defaultSpatialSpec<IntSize>()) +
                scaleOut(ms.defaultSpatialSpec(), targetScale = 0.85f, transformOrigin = TransformOrigin(0f, 0f)),
        ) {
            ControlPanel(state, engine)
        }

        Joystick(engine)
    }
}

@Composable
private fun TopPill(expanded: Boolean, onToggle: () -> Unit, onDrag: (Float, Float) -> Unit, onClose: () -> Unit) {
    val ms = MaterialTheme.motionScheme
    val last = remember { FloatArray(2) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, ms.fastSpatialSpec(), label = "chevron")

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            // Drag handle: uses raw screen coordinates so the window doesn't jitter while moving.
            Icon(
                painterResource(R.drawable.ic_drag), "Move",
                Modifier
                    .size(48.dp)
                    .padding(12.dp)
                    .pointerInteropFilter { e ->
                        when (e.actionMasked) {
                            MotionEvent.ACTION_DOWN -> { last[0] = e.rawX; last[1] = e.rawY }
                            MotionEvent.ACTION_MOVE -> {
                                onDrag(e.rawX - last[0], e.rawY - last[1])
                                last[0] = e.rawX; last[1] = e.rawY
                            }
                        }
                        true
                    },
            )
            IconButton(onClick = onToggle) {
                Icon(painterResource(R.drawable.ic_expand), "Toggle panel", Modifier.rotate(rotation))
            }
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), "Close overlay")
            }
        }
    }
}

@Composable
private fun ControlPanel(state: MockEngine.State, engine: MockEngine) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.width(288.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                String.format(Locale.US, "%.5f, %.5f", state.lat, state.lon),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                String.format(Locale.US, "%.1f km/h  |  %d\u00B0", state.currentSpeed * 3.6f, state.bearing.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SingleChoiceSegmentedButtonRow(Modifier.width(256.dp)) {
                presets.forEachIndexed { i, (label, v) ->
                    SegmentedButton(
                        selected = state.speedSetting == v,
                        onClick = { engine.setSpeed(v) },
                        shape = SegmentedButtonDefaults.itemShape(i, presets.size),
                        icon = {},
                        label = { Text(label, fontSize = 12.sp, maxLines = 1) },
                    )
                }
            }

            FilledTonalButton(onClick = { engine.setPaused(!state.paused) }) {
                Icon(
                    painterResource(if (state.paused) R.drawable.ic_play else R.drawable.ic_pause),
                    null, Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (state.paused) "Resume" else "Pause")
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Joystick(engine: MockEngine) {
    val ms = MaterialTheme.motionScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val baseSize = 168.dp
    val knobSize = 68.dp
    val center = with(density) { baseSize.toPx() / 2f }
    val maxR = with(density) { (baseSize - knobSize).toPx() / 2f }

    val knob = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 1.15f else 1f, ms.fastSpatialSpec(), label = "knobScale")
    val corner by animateFloatAsState(if (pressed) 32f else 50f, ms.defaultSpatialSpec(), label = "knobShape")
    val ringColor = MaterialTheme.colorScheme.outlineVariant

    fun release() {
        pressed = false
        engine.setJoystick(0f, 0f)
        // Spring back to the center with the expressive bounce.
        scope.launch { knob.animateTo(Offset.Zero, ms.defaultSpatialSpec()) }
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Box(
            Modifier
                .size(baseSize)
                .pointerInput(maxR) {
                    fun move(p: Offset) {
                        var v = p - Offset(center, center)
                        val len = v.getDistance()
                        if (len > maxR) v *= (maxR / len)
                        scope.launch { knob.snapTo(v) }
                        engine.setJoystick(v.x / maxR, v.y / maxR)
                    }
                    detectDragGestures(
                        onDragStart = { pressed = true; move(it) },
                        onDragEnd = { release() },
                        onDragCancel = { release() },
                    ) { change, _ -> change.consume(); move(change.position) }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(baseSize)) {
                drawCircle(
                    color = ringColor,
                    radius = size.minDimension / 2f - 6.dp.toPx(),
                    style = Stroke(2.dp.toPx()),
                )
            }
            val shape = RoundedCornerShape(percent = corner.roundToInt().coerceIn(0, 50))
            Box(
                Modifier
                    .offset { IntOffset(knob.value.x.roundToInt(), knob.value.y.roundToInt()) }
                    .size(knobSize)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .shadow(4.dp, shape)
                    .background(MaterialTheme.colorScheme.primary, shape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_gps), null,
                    Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
