package cz.majkey.pocasicesko.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import cz.majkey.pocasicesko.data.WeatherKind
import cz.majkey.pocasicesko.widget.widgetArgbOrNull
import cz.majkey.pocasicesko.widget.widgetContrastRatio
import kotlin.math.roundToInt

internal data class WeatherPalette(
    val background: List<Color>,
    val primaryGlow: Color = Color.Transparent,
    val secondaryGlow: Color = Color.Transparent,
)

internal fun weatherPalette(kind: WeatherKind?, isDay: Boolean): WeatherPalette =
    appearancePalette(AppAppearance.WEATHER, kind, isDay)

internal fun appearancePalette(
    appearance: AppAppearance,
    kind: WeatherKind?,
    isDay: Boolean,
    dark: Boolean = true,
    custom: CustomAppearance = CustomAppearance(),
): WeatherPalette {
    fun gradient(lightStart: Long, lightEnd: Long, darkStart: Long, darkEnd: Long) = WeatherPalette(
        if (dark) listOf(Color(darkStart), Color(darkEnd)) else listOf(Color(lightStart), Color(lightEnd)))
    return when (appearance) {
        AppAppearance.WEATHER -> when {
            !isDay -> gradient(0xFFE5E8F5, 0xFFF6F7FC, 0xFF111A33, 0xFF080D1A)
            kind == WeatherKind.CLEAR || kind == WeatherKind.MAINLY_CLEAR ->
                gradient(0xFFFFEED2, 0xFFF9F7F1, 0xFF593426, 0xFF19151D)
            kind == WeatherKind.RAIN || kind == WeatherKind.STORM ->
                gradient(0xFFDCE7EE, 0xFFF4F7FA, 0xFF263A49, 0xFF101D28)
            kind == WeatherKind.SNOW -> gradient(0xFFE4EFF7, 0xFFF9FCFF, 0xFF233648, 0xFF101924)
            else -> gradient(0xFFDCEBF3, 0xFFF4F8FA, 0xFF24465C, 0xFF10202D)
        }
        AppAppearance.OCEAN -> gradient(0xFFDCEFF3, 0xFFF4FAFB, 0xFF1D4B61, 0xFF102A3C)
        AppAppearance.SUNSET -> gradient(0xFFFFE9D7, 0xFFFFF7F0, 0xFF593426, 0xFF231923)
        AppAppearance.FOREST -> gradient(0xFFE4EEE5, 0xFFF5F8F3, 0xFF234D43, 0xFF132D29)
        AppAppearance.SAND -> gradient(0xFFF1E8D7, 0xFFFAF8F2, 0xFF49402E, 0xFF211E17)
        AppAppearance.LAVENDER -> gradient(0xFFECE6F6, 0xFFFAF7FF, 0xFF393047, 0xFF171321)
        AppAppearance.ROSE -> gradient(0xFFF4E2E8, 0xFFFFF8FA, 0xFF4E2C3B, 0xFF20151D)
        AppAppearance.SLATE -> gradient(0xFFE4EBEF, 0xFFF7F9FA, 0xFF29343E, 0xFF111820)
        AppAppearance.MATERIAL -> gradient(0xFFF5F7FB, 0xFFF5F7FB, 0xFF171C22, 0xFF171C22)
        AppAppearance.MINIMAL -> gradient(0xFFF7F7F5, 0xFFF7F7F5, 0xFF121416, 0xFF121416)
        AppAppearance.AMOLED -> gradient(0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000)
        AppAppearance.CUSTOM -> WeatherPalette(listOf(custom.backgroundStart.color(), custom.backgroundEnd.color()))
    }
}

internal fun customAppearanceReadable(custom: CustomAppearance): Boolean {
    val values = listOf(custom.backgroundStart, custom.backgroundEnd, custom.surface,
        custom.accent, custom.primaryText, custom.secondaryText)
    if (values.any { widgetArgbOrNull(it)?.ushr(24) != 255 }) return false
    val backgrounds = gradientContrastSamples(listOf(custom.backgroundStart.color(), custom.backgroundEnd.color())) + custom.surface.color()
    val primary = if (custom.automaticText) readableForeground(backgrounds) else custom.primaryText.color()
    val secondary = if (custom.automaticText) primary else custom.secondaryText.color()
    return backgrounds.all { contrast(primary, it) >= TEXT_CONTRAST && contrast(secondary, it) >= TEXT_CONTRAST }
}

internal fun appearanceColors(
    palette: WeatherPalette,
    appearance: AppAppearance,
    dark: Boolean,
    custom: CustomAppearance = CustomAppearance(),
): ColorScheme {
    val surface = if (appearance == AppAppearance.CUSTOM) custom.surface.color()
        else if (dark) lerp(palette.background.last(), Color.White, if (appearance == AppAppearance.AMOLED) 0f else 0.035f)
        else Color(0xFFFAFCFD)
    val backgrounds = gradientContrastSamples(palette.background) + surface
    val text = if (appearance == AppAppearance.CUSTOM && !custom.automaticText) custom.primaryText.color()
        else readableForeground(backgrounds)
    val mutedCandidate = if (appearance == AppAppearance.CUSTOM && !custom.automaticText) custom.secondaryText.color()
        else lerp(text, surface, 0.24f)
    val muted = if (backgrounds.all { contrast(mutedCandidate, it) >= TEXT_CONTRAST }) mutedCandidate else text
    val seed = if (appearance == AppAppearance.CUSTOM) custom.accent.color() else when (appearance) {
        AppAppearance.SUNSET, AppAppearance.SAND -> Color(0xFFA96922)
        AppAppearance.FOREST -> Color(0xFF347263)
        AppAppearance.LAVENDER -> Color(0xFF7357A8)
        AppAppearance.ROSE -> Color(0xFF9B476C)
        AppAppearance.MINIMAL, AppAppearance.AMOLED -> text
        else -> Color(0xFF3B718E)
    }
    val accent = (0..10).map { lerp(seed, text, it / 10f) }
        .firstOrNull { color -> backgrounds.all { contrast(color, it) >= TEXT_CONTRAST } } ?: text
    val variant = if (appearance == AppAppearance.CUSTOM) surface else lerp(surface, accent, if (dark) 0.12f else 0.06f)
    val base = if (text.luminance() > 0.5f) darkColorScheme() else lightColorScheme()
    return base.copy(primary = accent, onPrimary = readableForeground(listOf(accent)),
        primaryContainer = variant, onPrimaryContainer = text,
        secondary = accent, onSecondary = readableForeground(listOf(accent)),
        secondaryContainer = variant, onSecondaryContainer = text,
        background = palette.background.last(), onBackground = text,
        surface = surface, onSurface = text, surfaceVariant = variant, onSurfaceVariant = muted,
        surfaceDim = surface, surfaceBright = surface, surfaceContainerLowest = surface,
        surfaceContainerLow = surface, surfaceContainer = surface, surfaceContainerHigh = variant,
        surfaceContainerHighest = variant, surfaceTint = accent,
        inverseSurface = text, inverseOnSurface = surface, inversePrimary = readableForeground(listOf(text)),
        outline = lerp(surface, text, 0.32f), outlineVariant = lerp(surface, text, 0.12f))
}

private fun String.color(): Color = Color(widgetArgbOrNull(this) ?: 0xFF102332.toInt())
private fun contrast(text: Color, background: Color): Double = widgetContrastRatio(text.toArgb(), background.toArgb())
private fun readableForeground(backgrounds: List<Color>): Color =
    listOf(Color(0xFF17222B), Color.White).maxBy { candidate -> backgrounds.minOf { contrast(candidate, it) } }

// ponytail: 33 sRGB samples plus margin; use analytic extrema if arbitrary color spaces are supported.
internal fun gradientContrastSamples(stops: List<Color>): List<Color> = stops.zipWithNext().flatMap { (first, second) ->
    val start = first.toArgb()
    val end = second.toArgb()
    (0..32).map { step ->
        fun channel(shift: Int): Int {
            val a = (start ushr shift) and 255
            val b = (end ushr shift) and 255
            return (a + (b - a) * step / 32f).roundToInt()
        }
        Color(0xFF000000.toInt() or (channel(16) shl 16) or (channel(8) shl 8) or channel(0))
    }
}.ifEmpty { stops }

private const val TEXT_CONTRAST = 4.6
