package com.orca.kegel.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orca.kegel.domain.AssessmentResult
import com.orca.kegel.domain.EhsRecord
import com.orca.kegel.domain.Level
import com.orca.kegel.domain.LevelChange
import com.orca.kegel.domain.LevelRules
import com.orca.kegel.domain.SessionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "kegel_prefs")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class LangMode { SYSTEM, ZH, EN }

/** 全部本地状态（设置 / 等级 / 摸底 / 记录 / EHS） */
data class AppState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: LangMode = LangMode.SYSTEM,
    val dailyFrequency: Int = 2,
    val silentMode: Boolean = false,
    val ehsEnabled: Boolean = false,
    val currentLevel: Level = Level.L1,
    val levelChosen: Boolean = false,
    val lastAssessment: AssessmentResult? = null,
    val levelChanges: List<LevelChange> = emptyList(),
    val history: List<SessionRecord> = emptyList(),
    val ehsRecords: List<EhsRecord> = emptyList(),
    val retestPromptDismissed: Boolean = false,
) {
    fun historyNewestFirst(): List<SessionRecord> = history.sortedByDescending { it.timestampMs }

    fun sessionsOn(epochDay: Long): Int = history.count { it.epochDay == epochDay }

    fun contractionsOn(epochDay: Long): Int =
        history.filter { it.epochDay == epochDay }.sumOf { it.enduranceReps + it.fastReps }

    /** 当日剩余额度（日上限 40 次收缩） */
    fun remainingToday(epochDay: Long = LocalDate.now().toEpochDay()): Int =
        (LevelRules.DAILY_CONTRACTION_LIMIT - contractionsOn(epochDay)).coerceAtLeast(0)

    /** 连续完成天数（从今天或昨天往前数） */
    fun consecutiveCompletedDays(): Int {
        val days = history.filter { it.completed }.map { it.epochDay }.toSet()
        if (days.isEmpty()) return 0
        var d = LocalDate.now().toEpochDay()
        if (d !in days) d -= 1
        var count = 0
        while (d in days) {
            count++
            d -= 1
        }
        return count
    }

    /** 距上次摸底之后的完成日数（达 14 提示一次重测） */
    fun completedDaysSinceAssessment(): Int {
        val a = lastAssessment ?: return 0
        return history.filter { it.completed && it.epochDay > a.epochDay }
            .map { it.epochDay }.distinct().size
    }

    fun shouldPromptRestDay(): Boolean =
        consecutiveCompletedDays() >= LevelRules.REST_DAY_AFTER_CONSECUTIVE_DAYS

    fun shouldPromptRetest(): Boolean =
        !retestPromptDismissed &&
            lastAssessment != null &&
            completedDaysSinceAssessment() >= LevelRules.RETEST_PROMPT_AFTER_DAYS
}

class AppRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object K {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val FREQUENCY = intPreferencesKey("daily_frequency")
        val SILENT = booleanPreferencesKey("silent_mode")
        val EHS_ENABLED = booleanPreferencesKey("ehs_enabled")
        val LEVEL = intPreferencesKey("current_level")
        val LEVEL_CHOSEN = booleanPreferencesKey("level_chosen")
        val ASSESSMENT = stringPreferencesKey("last_assessment")
        val LEVEL_CHANGES = stringPreferencesKey("level_changes")
        val HISTORY = stringPreferencesKey("history")
        val EHS = stringPreferencesKey("ehs_records")
        val RETEST_DISMISSED = booleanPreferencesKey("retest_dismissed")
    }

    val state: Flow<AppState> = context.dataStore.data.map { p -> p.toState() }

    private fun Preferences.toState(): AppState = AppState(
        theme = runCatching { ThemeMode.valueOf(this[K.THEME] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
        language = runCatching { LangMode.valueOf(this[K.LANGUAGE] ?: "") }.getOrDefault(LangMode.SYSTEM),
        dailyFrequency = (this[K.FREQUENCY] ?: 2).coerceIn(1, 3),
        silentMode = this[K.SILENT] ?: false,
        ehsEnabled = this[K.EHS_ENABLED] ?: false,
        currentLevel = Level.of(this[K.LEVEL] ?: 1),
        levelChosen = this[K.LEVEL_CHOSEN] ?: false,
        lastAssessment = this[K.ASSESSMENT]?.let { s -> runCatching { json.decodeFromString<AssessmentResult>(s) }.getOrNull() },
        levelChanges = this[K.LEVEL_CHANGES]?.let { s -> runCatching { json.decodeFromString<List<LevelChange>>(s) }.getOrNull() } ?: emptyList(),
        history = this[K.HISTORY]?.let { s -> runCatching { json.decodeFromString<List<SessionRecord>>(s) }.getOrNull() } ?: emptyList(),
        ehsRecords = this[K.EHS]?.let { s -> runCatching { json.decodeFromString<List<EhsRecord>>(s) }.getOrNull() } ?: emptyList(),
        retestPromptDismissed = this[K.RETEST_DISMISSED] ?: false,
    )

    suspend fun setTheme(mode: ThemeMode) = edit { it[K.THEME] = mode.name }
    suspend fun setLanguage(mode: LangMode) = edit { it[K.LANGUAGE] = mode.name }
    suspend fun setFrequency(times: Int) = edit { it[K.FREQUENCY] = times.coerceIn(1, 3) }
    suspend fun setSilentMode(on: Boolean) = edit { it[K.SILENT] = on }
    suspend fun setEhsEnabled(on: Boolean) = edit { it[K.EHS_ENABLED] = on }

    suspend fun setLevel(level: Level, reason: String) = edit { p ->
        val from = Level.of(p[K.LEVEL] ?: 1)
        p[K.LEVEL] = level.num
        p[K.LEVEL_CHOSEN] = true
        if (from != level) {
            val list = p[K.LEVEL_CHANGES]?.let { s -> runCatching { json.decodeFromString<List<LevelChange>>(s) }.getOrNull() } ?: emptyList()
            val updated = list + LevelChange(LocalDate.now().toEpochDay(), from, level, reason)
            p[K.LEVEL_CHANGES] = json.encodeToString(updated)
        }
    }

    suspend fun saveAssessment(result: AssessmentResult) = edit { p ->
        p[K.ASSESSMENT] = json.encodeToString(result)
        p[K.RETEST_DISMISSED] = false
    }

    suspend fun dismissRetestPrompt() = edit { it[K.RETEST_DISMISSED] = true }

    suspend fun addSession(record: SessionRecord) = edit { p ->
        val list = p[K.HISTORY]?.let { s -> runCatching { json.decodeFromString<List<SessionRecord>>(s) }.getOrNull() } ?: emptyList()
        p[K.HISTORY] = json.encodeToString(list + record)
    }

    suspend fun addEhs(record: EhsRecord) = edit { p ->
        val list = p[K.EHS]?.let { s -> runCatching { json.decodeFromString<List<EhsRecord>>(s) }.getOrNull() } ?: emptyList()
        p[K.EHS] = json.encodeToString(list.filter { it.epochDay != record.epochDay } + record)
    }

    suspend fun clearEhs() = edit { it.remove(K.EHS) }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}