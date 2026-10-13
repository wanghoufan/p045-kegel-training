package com.orca.kegel.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orca.kegel.domain.AssessmentResult
import com.orca.kegel.domain.DailyAllocation
import com.orca.kegel.domain.EhsRecord
import com.orca.kegel.domain.Level
import com.orca.kegel.domain.LevelChange
import com.orca.kegel.domain.LevelRules
import com.orca.kegel.domain.Quota
import com.orca.kegel.domain.SessionPlan
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

/** 全部本地状态（设置 / 等级 / 摸底 / 记录 / EHS / 当日额度） */
data class AppState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: LangMode = LangMode.SYSTEM,
    /** 提醒次数 1/2/3（默认 2），与训练会话次数分开 */
    val reminderTimes: Int = 2,
    /** 训练会话次数 1/2/3/4/6；SESSIONS_AUTO＝按等级默认 */
    val sessionCount: Int = LevelRules.SESSIONS_AUTO,
    val silentMode: Boolean = false,
    val ehsEnabled: Boolean = false,
    val currentLevel: Level = Level.L1,
    val levelChosen: Boolean = false,
    val lastAssessment: AssessmentResult? = null,
    val levelChanges: List<LevelChange> = emptyList(),
    val history: List<SessionRecord> = emptyList(),
    val ehsRecords: List<EhsRecord> = emptyList(),
    val retestPromptDismissed: Boolean = false,
    /** 当日半量冻结：仅在 halfDoseDate 当天生效 */
    val halfDoseDate: Long = 0,
    val halfDoseValue: Boolean = false,
    /** 本地日首次规划冻结的 Level / 会话数（当天只收不放，不因调高而抬额） */
    val freezeDate: Long = 0,
    val freezeLevelNum: Int = 0,
    val freezeSessions: Int = 0,
    val doseVersion: Int = SessionPlan.DOSE_VERSION,
    /** 摸底实际已开始的耐力收缩（按发生日；不计完成会话、不参与统计） */
    val assessmentDate: Long = 0,
    val assessmentUsedE: Int = 0,
    /** 安全原因（如疲劳）自动安排的次日半量（仅 pendingHalfDate 当天生效） */
    val pendingHalfDate: Long = 0,
    val pendingHalfValue: Boolean = false,
) {
    fun historyNewestFirst(): List<SessionRecord> = history.sortedByDescending { it.timestampMs }

    fun sessionsOn(epochDay: Long): Int = history.count { it.epochDay == epochDay }

    /** 当日实际已开始的耐力收缩（中止不返还；含摸底；跨午夜按发生日归属） */
    fun usedE(epochDay: Long): Int {
        val fromSessions = history.sumOf { r ->
            if (r.startedByDay.isNotEmpty()) {
                r.startedByDay.filter { it.epochDay == epochDay }.sumOf { it.enduranceStarted }
            } else if (r.epochDay == epochDay) {
                maxOf(r.enduranceStarted, r.enduranceReps)
            } else 0
        }
        val fromAssessment = if (assessmentDate == epochDay) assessmentUsedE else 0
        return fromSessions + fromAssessment
    }

    /** 当日实际已开始的快速收缩（跨午夜按发生日归属） */
    fun usedF(epochDay: Long): Int = history.sumOf { r ->
        if (r.startedByDay.isNotEmpty()) {
            r.startedByDay.filter { it.epochDay == epochDay }.sumOf { it.fastStarted }
        } else if (r.epochDay == epochDay) {
            maxOf(r.fastStarted, r.fastReps)
        } else 0
    }

    fun contractionsOn(epochDay: Long): Int = usedE(epochDay) + usedF(epochDay)

    /** 本次是当日第几个时段（1-based） */
    fun nextSessionIndex(epochDay: Long = LocalDate.now().toEpochDay()): Int =
        sessionsOn(epochDay) + 1

    /** 生效会话次数（非法/自动值按等级默认） */
    fun effectiveSessions(level: Level = currentLevel): Int =
        LevelRules.resolveSessions(level, sessionCount)

    /** 当日生效等级：已冻结则取「冻结与当前中较低者」（可降不可升） */
    fun dayLevel(epochDay: Long): Level {
        if (freezeDate != epochDay || freezeLevelNum <= 0) return currentLevel
        return Level.of(minOf(freezeLevelNum, currentLevel.num))
    }

    /** 当日生效会话数：已冻结则取「冻结与当前中较低者」 */
    fun daySessions(epochDay: Long): Int {
        val live = effectiveSessions(currentLevel)
        if (freezeDate != epochDay || freezeSessions <= 0) return live
        return minOf(freezeSessions, live)
    }

    /** 当日是否处于半量（冻结语义：当日冻结或安全安排的次日半量） */
    fun halfDoseOn(epochDay: Long = LocalDate.now().toEpochDay()): Boolean =
        (halfDoseDate == epochDay && halfDoseValue) ||
            (pendingHalfDate == epochDay && pendingHalfValue)

    fun allocation(
        epochDay: Long = LocalDate.now().toEpochDay(),
    ): DailyAllocation =
        Quota.daily(dayLevel(epochDay), daySessions(epochDay), halfDoseOn(epochDay))

    fun remainingE(epochDay: Long = LocalDate.now().toEpochDay()): Int =
        (allocation(epochDay).eQuota - usedE(epochDay)).coerceAtLeast(0)

    fun remainingF(epochDay: Long = LocalDate.now().toEpochDay()): Int =
        (allocation(epochDay).fQuota - usedF(epochDay)).coerceAtLeast(0)

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

    /** 连续高量日数（当日实际已开始 > 120），从今天或昨天往前数 */
    fun consecutiveHighDays(): Int {
        val high = history.map { it.epochDay }.distinct()
            .filter { contractionsOn(it) > LevelRules.HIGH_DAY_THRESHOLD }
            .toSet()
        if (high.isEmpty()) return 0
        var d = LocalDate.now().toEpochDay()
        if (d !in high) d -= 1
        var count = 0
        while (d in high) {
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
        consecutiveCompletedDays() >= LevelRules.REST_DAY_AFTER_CONSECUTIVE_DAYS ||
            consecutiveHighDays() >= LevelRules.HIGH_DAY_CONSECUTIVE

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
        val REMINDER = intPreferencesKey("reminder_times")

        // V1 旧键：每日提醒次数（旧「频率」），仅迁移读取
        val FREQUENCY_LEGACY = intPreferencesKey("daily_frequency")
        val SESSIONS = intPreferencesKey("session_count")
        val SILENT = booleanPreferencesKey("silent_mode")
        val EHS_ENABLED = booleanPreferencesKey("ehs_enabled")
        val LEVEL = intPreferencesKey("current_level")
        val LEVEL_CHOSEN = booleanPreferencesKey("level_chosen")
        val ASSESSMENT = stringPreferencesKey("last_assessment")
        val LEVEL_CHANGES = stringPreferencesKey("level_changes")
        val HISTORY = stringPreferencesKey("history")
        val EHS = stringPreferencesKey("ehs_records")
        val RETEST_DISMISSED = booleanPreferencesKey("retest_dismissed")
        val HALF_DATE = longPreferencesKey("half_dose_date")
        val HALF_VALUE = booleanPreferencesKey("half_dose_value")
        val FREEZE_DATE = longPreferencesKey("freeze_date")
        val FREEZE_LEVEL = intPreferencesKey("freeze_level")
        val FREEZE_SESSIONS = intPreferencesKey("freeze_sessions")
        val DOSE_VERSION = intPreferencesKey("dose_version")
        val ASSESS_DATE = longPreferencesKey("assessment_quota_date")
        val ASSESS_USED_E = intPreferencesKey("assessment_quota_used_e")
        val PENDING_HALF_DATE = longPreferencesKey("pending_half_date")
        val PENDING_HALF_VALUE = booleanPreferencesKey("pending_half_value")
    }

    val state: Flow<AppState> = context.dataStore.data.map { p -> p.toState() }

    private fun Preferences.decodeHistory(): List<SessionRecord> =
        this[K.HISTORY]?.let { s -> runCatching { json.decodeFromString<List<SessionRecord>>(s) }.getOrNull() } ?: emptyList()

    private fun Preferences.toState(): AppState = AppState(
        theme = runCatching { ThemeMode.valueOf(this[K.THEME] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
        language = runCatching { LangMode.valueOf(this[K.LANGUAGE] ?: "") }.getOrDefault(LangMode.SYSTEM),
        reminderTimes = (this[K.REMINDER] ?: this[K.FREQUENCY_LEGACY] ?: 2)
            .let { if (it in LevelRules.reminderOptions) it else 2 },
        sessionCount = (this[K.SESSIONS] ?: LevelRules.SESSIONS_AUTO)
            .let { if (it in LevelRules.sessionOptions) it else LevelRules.SESSIONS_AUTO },
        silentMode = this[K.SILENT] ?: false,
        ehsEnabled = this[K.EHS_ENABLED] ?: false,
        currentLevel = Level.of(this[K.LEVEL] ?: 1),
        levelChosen = this[K.LEVEL_CHOSEN] ?: false,
        lastAssessment = this[K.ASSESSMENT]?.let { s -> runCatching { json.decodeFromString<AssessmentResult>(s) }.getOrNull() },
        levelChanges = this[K.LEVEL_CHANGES]?.let { s -> runCatching { json.decodeFromString<List<LevelChange>>(s) }.getOrNull() } ?: emptyList(),
        history = decodeHistory(),
        ehsRecords = this[K.EHS]?.let { s -> runCatching { json.decodeFromString<List<EhsRecord>>(s) }.getOrNull() } ?: emptyList(),
        retestPromptDismissed = this[K.RETEST_DISMISSED] ?: false,
        halfDoseDate = this[K.HALF_DATE] ?: 0L,
        halfDoseValue = this[K.HALF_VALUE] ?: false,
        freezeDate = this[K.FREEZE_DATE] ?: 0L,
        freezeLevelNum = this[K.FREEZE_LEVEL] ?: 0,
        freezeSessions = this[K.FREEZE_SESSIONS] ?: 0,
        doseVersion = this[K.DOSE_VERSION] ?: SessionPlan.DOSE_VERSION,
        assessmentDate = this[K.ASSESS_DATE] ?: 0L,
        assessmentUsedE = this[K.ASSESS_USED_E] ?: 0,
        pendingHalfDate = this[K.PENDING_HALF_DATE] ?: 0L,
        pendingHalfValue = this[K.PENDING_HALF_VALUE] ?: false,
    )

    suspend fun setTheme(mode: ThemeMode) = edit { it[K.THEME] = mode.name }
    suspend fun setLanguage(mode: LangMode) = edit { it[K.LANGUAGE] = mode.name }

    suspend fun setReminderTimes(times: Int) = edit {
        it[K.REMINDER] = if (times in LevelRules.reminderOptions) times else 2
    }

    suspend fun setSessions(times: Int) = edit {
        it[K.SESSIONS] = if (times in LevelRules.sessionOptions) times else LevelRules.SESSIONS_AUTO
    }

    suspend fun setSilentMode(on: Boolean) = edit { it[K.SILENT] = on }
    suspend fun setEhsEnabled(on: Boolean) = edit { it[K.EHS_ENABLED] = on }

    /**
     * 当日半量冻结：开启即时生效；当天已启动过训练则不允许关闭（不抬冻结额，次日生效），
     * 未启动过则可自由开关。
     */
    suspend fun setHalfDose(epochDay: Long, on: Boolean) = edit { p ->
        val currentDate = p[K.HALF_DATE] ?: 0L
        val currentValue = p[K.HALF_VALUE] ?: false
        if (on) {
            p[K.HALF_DATE] = epochDay
            p[K.HALF_VALUE] = true
        } else {
            val startedToday = p.decodeHistory().count { it.epochDay == epochDay }
            // 关闭当日半量：清理安全原因安排的当日待生效半量，允许用户显式关闭
            if ((p[K.PENDING_HALF_DATE] ?: 0L) == epochDay) p[K.PENDING_HALF_VALUE] = false
            if (currentDate == epochDay && currentValue && startedToday > 0) {
                // 已冻结且有已启动时段 → 不抬额，保持半量至次日
            } else {
                p[K.HALF_DATE] = epochDay
                p[K.HALF_VALUE] = false
            }
        }
    }

    /**
     * 安全原因（如疲劳停止）后自动安排次日整日半量：仅在目标日生效，不影响当日。
     * 与当日手动半量分开存储，避免覆盖/抬高当日冻结额。
     */
    suspend fun scheduleHalfDose(epochDay: Long) = edit { p ->
        p[K.PENDING_HALF_DATE] = epochDay
        p[K.PENDING_HALF_VALUE] = true
    }

    /**
     * 摸底实际已开始的耐力收缩计入当日额度（不计完成会话、不参与统计）。
     * 同日累加；跨日自动重置。
     */
    suspend fun addAssessmentUsage(epochDay: Long, enduranceStarted: Int) = edit { p ->
        if (enduranceStarted <= 0) return@edit
        val used = if ((p[K.ASSESS_DATE] ?: 0L) == epochDay) (p[K.ASSESS_USED_E] ?: 0) else 0
        p[K.ASSESS_DATE] = epochDay
        p[K.ASSESS_USED_E] = used + enduranceStarted
    }

    /**
     * 本地日首次规划冻结 Level 与会话数（当天只收不放：调高不抬额，调低立即收紧）。
     * 当日已冻结则不覆盖。
     */
    suspend fun freezeDose(epochDay: Long, level: Level, sessions: Int) = edit { p ->
        if ((p[K.FREEZE_DATE] ?: 0L) != epochDay) {
            p[K.FREEZE_DATE] = epochDay
            p[K.FREEZE_LEVEL] = level.num
            p[K.FREEZE_SESSIONS] = sessions
        }
    }

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
        p[K.HISTORY] = json.encodeToString(p.decodeHistory() + record)
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
