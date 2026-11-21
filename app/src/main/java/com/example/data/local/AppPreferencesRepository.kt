package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Langues prises en charge par l'interface de Devoirs TN.
 * L'arabe active automatiquement la mise en page de droite à gauche (RTL).
 */
enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val isRtl: Boolean
) {
    FR(code = "fr", nativeLabel = "Français", isRtl = false),
    AR(code = "ar", nativeLabel = "العربية", isRtl = true),
    EN(code = "en", nativeLabel = "English", isRtl = false);

    /** Construit la Locale Java/Android correspondante pour la résolution des ressources. */
    fun toLocale(): Locale = when (this) {
        FR -> Locale.FRENCH
        AR -> Locale("ar", "TN")
        EN -> Locale.ENGLISH
    }

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: FR
    }
}

/**
 * Modes de thème disponibles : clair, sombre ou synchronisation avec le système Android.
 */
enum class ThemeMode(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromId(id: String?): ThemeMode =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
    }
}

/**
 * État immuable des préférences globales de l'application :
 * langue, thème, objectif hebdomadaire, autorisation des données mobiles pour les téléchargements
 * et métadonnées du dernier catalogue synchronisé.
 */
data class AppPreferencesState(
    val language: AppLanguage = AppLanguage.FR,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weeklyGoalTarget: Int = 3,
    val allowMobileDataDownloads: Boolean = false,
    val cachedCatalogueVersion: Int = 0,
    val lastCatalogueSyncMillis: Long = 0L
)

/**
 * Dépôt léger de préférences persistées localement via SharedPreferences.
 * La lecture initiale est synchrone afin d'éviter tout flash visuel de thème ou de langue au démarrage.
 */
class AppPreferencesRepository(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<AppPreferencesState> = _state.asStateFlow()

    private fun loadInitialState(): AppPreferencesState {
        val savedLang = AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, AppLanguage.FR.code))
        val savedTheme = ThemeMode.fromId(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.id))
        val savedGoal = prefs.getInt(KEY_WEEKLY_GOAL, DEFAULT_WEEKLY_GOAL).coerceIn(1, 20)
        val allowMobileData = prefs.getBoolean(KEY_ALLOW_MOBILE_DATA, false)
        val catalogueVersion = prefs.getInt(KEY_CATALOGUE_VERSION, 0)
        val lastSync = prefs.getLong(KEY_LAST_CATALOGUE_SYNC, 0L)
        return AppPreferencesState(
            language = savedLang,
            themeMode = savedTheme,
            weeklyGoalTarget = savedGoal,
            allowMobileDataDownloads = allowMobileData,
            cachedCatalogueVersion = catalogueVersion,
            lastCatalogueSyncMillis = lastSync
        )
    }

    /** Met à jour et persiste la langue sélectionnée par l'élève. */
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _state.value = _state.value.copy(language = language)
    }

    /** Met à jour et persiste le mode d'affichage (Système, Clair ou Sombre). */
    fun setThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode.id).apply()
        _state.value = _state.value.copy(themeMode = themeMode)
    }

    /** Met à jour et persiste l'objectif hebdomadaire de devoirs (entre 1 et 20). */
    fun setWeeklyGoalTarget(target: Int) {
        val clamped = target.coerceIn(1, 20)
        prefs.edit().putInt(KEY_WEEKLY_GOAL, clamped).apply()
        _state.value = _state.value.copy(weeklyGoalTarget = clamped)
    }

    /**
     * Active ou désactive les téléchargements sur données mobiles (Wi-Fi uniquement par défaut).
     */
    fun setAllowMobileDataDownloads(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_MOBILE_DATA, allowed).apply()
        _state.value = _state.value.copy(allowMobileDataDownloads = allowed)
    }

    /** Enregistre la version et l'horodatage de la dernière synchronisation réussie du catalogue. */
    fun recordCatalogueSync(version: Int, syncedAtMillis: Long) {
        prefs.edit()
            .putInt(KEY_CATALOGUE_VERSION, version)
            .putLong(KEY_LAST_CATALOGUE_SYNC, syncedAtMillis)
            .apply()
        _state.value = _state.value.copy(
            cachedCatalogueVersion = version,
            lastCatalogueSyncMillis = syncedAtMillis
        )
    }

    companion object {
        private const val PREFS_NAME = "devoirs_tn_preferences"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_WEEKLY_GOAL = "weekly_goal_target"
        private const val KEY_ALLOW_MOBILE_DATA = "allow_mobile_data_downloads"
        private const val KEY_CATALOGUE_VERSION = "cached_catalogue_version"
        private const val KEY_LAST_CATALOGUE_SYNC = "last_catalogue_sync_millis"
        const val DEFAULT_WEEKLY_GOAL = 3
    }
}
