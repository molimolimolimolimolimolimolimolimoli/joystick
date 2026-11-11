@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.joyoverlay

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private fun c(ctx: Context, id: Int) = Color(ctx.getColor(id))

@SuppressLint("NewApi")
@Composable
fun OverlayTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val dynamic = ctx.resources.getBoolean(R.bool.use_dynamic_color) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Light/dark values come from customize/values and customize/values-night.
    val scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(ctx)
        dynamic -> dynamicLightColorScheme(ctx)
        else -> lightColorScheme(
            primary = c(ctx, R.color.md_primary),
            onPrimary = c(ctx, R.color.md_on_primary),
            primaryContainer = c(ctx, R.color.md_primary_container),
            onPrimaryContainer = c(ctx, R.color.md_on_primary_container),
            secondary = c(ctx, R.color.md_secondary),
            onSecondary = c(ctx, R.color.md_on_secondary),
            secondaryContainer = c(ctx, R.color.md_secondary_container),
            onSecondaryContainer = c(ctx, R.color.md_on_secondary_container),
            tertiary = c(ctx, R.color.md_tertiary),
            onTertiary = c(ctx, R.color.md_on_tertiary),
            tertiaryContainer = c(ctx, R.color.md_tertiary_container),
            onTertiaryContainer = c(ctx, R.color.md_on_tertiary_container),
            error = c(ctx, R.color.md_error),
            onError = c(ctx, R.color.md_on_error),
            background = c(ctx, R.color.md_background),
            onBackground = c(ctx, R.color.md_on_background),
            surface = c(ctx, R.color.md_surface),
            onSurface = c(ctx, R.color.md_on_surface),
            surfaceVariant = c(ctx, R.color.md_surface_variant),
            onSurfaceVariant = c(ctx, R.color.md_on_surface_variant),
            surfaceContainer = c(ctx, R.color.md_surface_container),
            surfaceContainerHigh = c(ctx, R.color.md_surface_container_high),
            outline = c(ctx, R.color.md_outline),
            outlineVariant = c(ctx, R.color.md_outline_variant),
        )
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
