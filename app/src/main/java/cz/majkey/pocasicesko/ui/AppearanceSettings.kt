package cz.majkey.pocasicesko.ui

import android.content.Context
import androidx.core.content.edit
import cz.majkey.pocasicesko.data.WeatherRepository
import cz.majkey.pocasicesko.widget.widgetHexOrNull

enum class AppAppearance {
    WEATHER,
    OCEAN,
    SUNSET,
    FOREST,
    MATERIAL,
    MINIMAL,
    SAND,
    LAVENDER,
    ROSE,
    SLATE,
    AMOLED,
    CUSTOM,
}

enum class AppearanceMode { SYSTEM, LIGHT, DARK }
enum class AppearanceFont { SYSTEM, SERIF, MONOSPACE }

data class CustomAppearance(
    val backgroundStart: String = "#17384A",
    val backgroundEnd: String = "#09131C",
    val surface: String = "#102332",
    val accent: String = "#8EDCF0",
    val primaryText: String = "#FFFFFF",
    val secondaryText: String = "#DDEAF1",
    val automaticText: Boolean = true,
)

data class AppearanceConfig(
    val mode: AppearanceMode = AppearanceMode.SYSTEM,
    val lightTheme: AppAppearance = AppAppearance.WEATHER,
    val darkTheme: AppAppearance = AppAppearance.WEATHER,
    val font: AppearanceFont = AppearanceFont.SYSTEM,
    val textScale: Float = 1f,
    val cornerRadius: Int = 16,
    val custom: CustomAppearance = CustomAppearance(),
) {
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        AppearanceMode.SYSTEM -> systemDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }

    fun theme(dark: Boolean): AppAppearance = if (dark) darkTheme else lightTheme

    fun normalized(): AppearanceConfig {
        val defaults = CustomAppearance()
        fun color(value: String, fallback: String): String = widgetHexOrNull(value)
            ?.takeIf { it.length == 7 || it.startsWith("#FF") } ?: fallback
        val colors = custom.copy(backgroundStart = color(custom.backgroundStart, defaults.backgroundStart),
            backgroundEnd = color(custom.backgroundEnd, defaults.backgroundEnd), surface = color(custom.surface, defaults.surface),
            accent = color(custom.accent, defaults.accent), primaryText = color(custom.primaryText, defaults.primaryText),
            secondaryText = color(custom.secondaryText, defaults.secondaryText))
        return copy(textScale = textScale.takeIf { it.isFinite() }?.coerceIn(0.9f, 1.25f) ?: 1f,
            cornerRadius = cornerRadius.coerceIn(8, 28),
            custom = if (customAppearanceReadable(colors)) colors else defaults)
    }
}

internal fun appAppearance(value: String?): AppAppearance =
    AppAppearance.entries.firstOrNull { it.name == value } ?: AppAppearance.WEATHER

object AppearanceSettings {
    fun load(context: Context): AppearanceConfig {
        val preferences = context.getSharedPreferences(WeatherRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val legacy = preferences.getString(KEY_APPEARANCE, null)
        val defaults = CustomAppearance()
        return AppearanceConfig(
            mode = AppearanceMode.entries.firstOrNull { it.name == preferences.getString("appearance_mode", null) } ?: AppearanceMode.SYSTEM,
            lightTheme = appAppearance(preferences.getString("appearance_light_theme", legacy)),
            darkTheme = appAppearance(preferences.getString("appearance_dark_theme", legacy)),
            font = AppearanceFont.entries.firstOrNull { it.name == preferences.getString("appearance_font", null) } ?: AppearanceFont.SYSTEM,
            textScale = preferences.getFloat("appearance_text_scale", 1f),
            cornerRadius = preferences.getInt("appearance_corner_radius", 16),
            custom = CustomAppearance(
                preferences.getString("appearance_custom_start", defaults.backgroundStart) ?: defaults.backgroundStart,
                preferences.getString("appearance_custom_end", defaults.backgroundEnd) ?: defaults.backgroundEnd,
                preferences.getString("appearance_custom_surface", defaults.surface) ?: defaults.surface,
                preferences.getString("appearance_custom_accent", defaults.accent) ?: defaults.accent,
                preferences.getString("appearance_custom_text", defaults.primaryText) ?: defaults.primaryText,
                preferences.getString("appearance_custom_muted", defaults.secondaryText) ?: defaults.secondaryText,
                preferences.getBoolean("appearance_automatic_text", true)),
        ).normalized()
    }

    fun save(context: Context, appearance: AppearanceConfig) {
        val settings = appearance.normalized()
        context.getSharedPreferences(WeatherRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_APPEARANCE, settings.darkTheme.name)
                putString("appearance_mode", settings.mode.name)
                putString("appearance_light_theme", settings.lightTheme.name)
                putString("appearance_dark_theme", settings.darkTheme.name)
                putString("appearance_font", settings.font.name)
                putFloat("appearance_text_scale", settings.textScale)
                putInt("appearance_corner_radius", settings.cornerRadius)
                putString("appearance_custom_start", settings.custom.backgroundStart)
                putString("appearance_custom_end", settings.custom.backgroundEnd)
                putString("appearance_custom_surface", settings.custom.surface)
                putString("appearance_custom_accent", settings.custom.accent)
                putString("appearance_custom_text", settings.custom.primaryText)
                putString("appearance_custom_muted", settings.custom.secondaryText)
                putBoolean("appearance_automatic_text", settings.custom.automaticText)
            }
    }

    private const val KEY_APPEARANCE = "app_appearance"
}
