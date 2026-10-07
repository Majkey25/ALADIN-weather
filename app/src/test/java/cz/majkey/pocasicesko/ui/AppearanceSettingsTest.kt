package cz.majkey.pocasicesko.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import cz.majkey.pocasicesko.data.WeatherKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import cz.majkey.pocasicesko.widget.widgetContrastRatio
import androidx.compose.ui.graphics.toArgb
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun everyLightAndDarkPresetKeepsTextAndNavigationReadable() {
        AppAppearance.entries.forEach { theme ->
            listOf(false, true).forEach { dark ->
                WeatherKind.entries.forEach { kind ->
                    val palette = appearancePalette(theme, kind, true, dark)
                    val colors = appearanceColors(palette, theme, dark)
                    (gradientContrastSamples(palette.background) + colors.surface + colors.surfaceVariant).forEach { background ->
                        assertTrue("$theme/$dark/$kind text", widgetContrastRatio(colors.onBackground.toArgb(), background.toArgb()) >= 4.5)
                        assertTrue("$theme/$dark/$kind muted", widgetContrastRatio(colors.onSurfaceVariant.toArgb(), background.toArgb()) >= 4.5)
                    }
                    assertEquals(1f, colors.surface.alpha, 0f)
                    assertEquals(1f, colors.primary.alpha, 0f)
                    assertTrue(widgetContrastRatio(colors.onPrimary.toArgb(), colors.primary.toArgb()) >= 4.5)
                }
            }
        }
    }

    @Test
    fun customWhiteBackgroundUsesDarkTextAndRejectsInvisibleManualText() {
        val white = CustomAppearance("#FFFFFF", "#F4F4F4", "#FFFFFF", "#1F698A", "#FFFFFF", "#FFFFFF", true)
        assertTrue(customAppearanceReadable(white))
        val palette = appearancePalette(AppAppearance.CUSTOM, null, true, false, white)
        val colors = appearanceColors(palette, AppAppearance.CUSTOM, false, white)
        assertTrue(widgetContrastRatio(colors.onBackground.toArgb(), Color.White.toArgb()) >= 4.5)
        assertFalse(customAppearanceReadable(white.copy(automaticText = false)))
        assertFalse(customAppearanceReadable(white.copy(backgroundStart = "#80FFFFFF")))
        assertFalse(customAppearanceReadable(white.copy(backgroundStart = "#000000")))
    }

    @Test
    fun customGradientChecksInteriorContrastNotOnlyEndpoints() {
        val crossing = CustomAppearance("#FF00FF", "#00CC00", "#FFFFFF", "#336699", automaticText = true)
        assertFalse(customAppearanceReadable(crossing))
    }

    @Test
    fun appearanceModesAreIndependentAndInvalidPreferencesAreBounded() {
        val config = AppearanceConfig(lightTheme = AppAppearance.SAND, darkTheme = AppAppearance.AMOLED)
        assertEquals(AppAppearance.SAND, config.theme(config.isDark(false)))
        assertEquals(AppAppearance.AMOLED, config.theme(config.isDark(true)))
        assertFalse(config.copy(mode = AppearanceMode.LIGHT).isDark(true))
        assertTrue(config.copy(mode = AppearanceMode.DARK).isDark(false))
        assertEquals(1f, config.copy(textScale = Float.NaN).normalized().textScale, 0f)
        assertEquals(28, config.copy(cornerRadius = 999).normalized().cornerRadius)
    }
    @Test
    fun missingOrUnsupportedPreferenceFallsBackToWeather() {
        assertEquals(AppAppearance.WEATHER, appAppearance(null))
        assertEquals(AppAppearance.WEATHER, appAppearance("unsupported"))
        assertEquals(AppAppearance.SUNSET, appAppearance("SUNSET"))
        assertEquals(AppAppearance.MINIMAL, appAppearance("MINIMAL"))
    }

    @Test
    fun fixedStylesIgnoreWeatherAndDaylightChanges() {
        AppAppearance.entries.filter { it != AppAppearance.WEATHER }.forEach { appearance ->
            val palette = appearancePalette(appearance, WeatherKind.CLEAR, true)
            assertEquals(palette, appearancePalette(appearance, WeatherKind.RAIN, false))
            assertEquals(palette, appearancePalette(appearance, null, true))
        }
    }

    @Test
    fun simpleStylesRenderSolidBackgroundsWithoutGlows() {
        listOf(AppAppearance.MATERIAL, AppAppearance.MINIMAL).forEach { appearance ->
            val palette = appearancePalette(appearance, WeatherKind.CLEAR, true)
            assertTrue(palette.background.size >= 2)
            assertEquals(1, palette.background.distinct().size)
            assertEquals(Color.Transparent, palette.primaryGlow)
            assertEquals(Color.Transparent, palette.secondaryGlow)
        }
    }

    @Test
    fun everyFixedStyleKeepsSecondaryTextReadableWithGlows() {
        AppAppearance.entries.filter { it != AppAppearance.WEATHER }.forEach { appearance ->
            val palette = appearancePalette(appearance, WeatherKind.CLEAR, true)
            val text = when (appearance) {
                AppAppearance.MATERIAL -> Color(0xFFCDDAE7)
                AppAppearance.MINIMAL -> Color(0xFFD4D6D8)
                else -> Color(0xFFDDEAF1)
            }
            palette.background.forEach { background ->
                val lit = palette.secondaryGlow.compositeOver(palette.primaryGlow.compositeOver(background))
                assertEquals(1f, lit.alpha, 0f)
                val contrast = (text.luminance() + 0.05f) / (lit.luminance() + 0.05f)
                assertTrue("$appearance secondary text contrast=$contrast", contrast >= 4.5f)
            }
        }
    }
}
