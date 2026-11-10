package com.potatameister.barenotes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

@Immutable
data class BareColors(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val textMuted: Color,
    val divider: Color,
    val icon: Color,
    val iconBg: Color
)

val LightBareColors = BareColors(
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF111111),
    textMuted = Color(0xFF777777),
    divider = Color(0xFFE8E8E8),
    icon = Color(0xFF111111),
    iconBg = Color(0xFF111111)
)

val DarkBareColors = BareColors(
    bg = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    text = Color(0xFFF5F5F5),
    textMuted = Color(0xFF9A9A9A),
    divider = Color(0xFF1F1F1F),
    icon = Color(0xFFF5F5F5),
    iconBg = Color(0xFF111111)
)

val LocalBareColors = staticCompositionLocalOf { LightBareColors }

object BareType {
    val Wordmark = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.08.em
    )
    val Eyebrow = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        letterSpacing = 0.2.em
    )
    val EditorTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    val ScreenTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
    val ListTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
    )
    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp
    )
    val Preview = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp
    )
    val Meta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp
    )
}

val BareShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

val BareTypography = Typography(
    titleLarge = BareType.ScreenTitle,
    bodyLarge = BareType.Body,
    bodyMedium = BareType.Body,
    labelLarge = BareType.Body,
    labelMedium = BareType.Meta
)

@Composable
fun BareTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkBareColors else LightBareColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.text,
            onPrimary = colors.bg,
            primaryContainer = colors.text,
            onPrimaryContainer = colors.bg,
            secondary = colors.textMuted,
            onSecondary = colors.bg,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.bg,
            onSurface = colors.text,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.textMuted,
            outline = colors.divider,
            outlineVariant = colors.divider,
            error = colors.text,
            onError = colors.bg
        )
    } else {
        lightColorScheme(
            primary = colors.text,
            onPrimary = colors.bg,
            primaryContainer = colors.text,
            onPrimaryContainer = colors.bg,
            secondary = colors.textMuted,
            onSecondary = colors.bg,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.bg,
            onSurface = colors.text,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.textMuted,
            outline = colors.divider,
            outlineVariant = colors.divider,
            error = colors.text,
            onError = colors.bg
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalBareColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BareTypography,
            shapes = BareShapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

val BareGutter = 20.dp
val BareSidebarPadding = 24.dp
