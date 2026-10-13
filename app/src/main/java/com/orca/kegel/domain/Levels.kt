package com.orca.kegel.domain

/**
 * 训练等级 L1–L6。产品训练等级，非医学肌力等级。
 */
enum class Level(val num: Int) {
    L1(1), L2(2), L3(3), L4(4), L5(5), L6(6);

    val label: String get() = "L$num"

    companion object {
        fun of(num: Int): Level = entries.firstOrNull { it.num == num } ?: L1
        fun fromLabel(s: String?): Level? = entries.firstOrNull { it.label == s }
    }
}

/**
 * 六级剂量矩阵（PRODUCT_PLAN_V2.2，用户 2026-10-13 直接批准。
 * 全为产品设定、非临床建议；无任何外部许可门槛）。
 *
 * 设计口径（V2.2）：先定「每次会话做多少」，日量 = 每次次数 × 会话数，天然整除，
 * 每次会话次数全天一致（不存在 17/17/16 式余数分摊）。
 *
 * 字段：
 *  - holdSec   单次保持 H（含收紧起始，不再另加 2 秒），≤10
 *  - restSec   每次后完全放松 R
 *  - dailyEndurance 日耐力次数 E（= endurancePerSession × fullSessions）
 *  - fastSec   快速收紧秒
 *  - fastRestSec 快速放松 r（L1=4，L2–L6=2）
 *  - dailyFast 日快速次数 F（= fastPerSession × fullSessions）
 *  - fullSessions 满量会话数 N（3/3/4/4/5/6）
 *
 * 每次会话 = 1 个耐力组（≤10 次）+ 1 个快速回合（≤20 次），单次合计 ≤ 30 ≤ SESSION_MAX(40)。
 * 守恒：dailyEndurance + dailyFast = 日总 D，且 ≤ DAILY_LIMIT(240)。
 */
data class LevelSpec(
    val level: Level,
    val holdSec: Int,
    val restSec: Int,
    val dailyEndurance: Int,
    val fastSec: Int,
    val fastRestSec: Int,
    val dailyFast: Int,
    val fullSessions: Int,
) {
    val dailyTotal: Int get() = dailyEndurance + dailyFast

    /** 每次会话耐力次数 e（V2.2 单一真源：日量 ÷ 会话数）。 */
    val endurancePerSession: Int get() = if (fullSessions <= 0) 0 else dailyEndurance / fullSessions

    /** 每次会话快速次数 q（V2.2 单一真源：日量 ÷ 会话数）。 */
    val fastPerSession: Int get() = if (fullSessions <= 0) 0 else dailyFast / fullSessions
}

/** 初始/升级建议阈值（短板约束：三项全部达标才取该级）。 */
data class LevelThreshold(
    val level: Level,
    val holdSec: Int,
    val enduranceReps: Int,
    val fastReps: Int,
)

object LevelRules {

    /**
     * V2.2 满量矩阵（用户 2026-10-13 批准）。每级每次会话固定次数：
     *   每次耐力 e / 每次快速 q / 会话数 N → 日耐力 e×N / 日快速 q×N / 日总。
     *   L1 5/5/3=15+15=30；L2 6/8/3=18+24=42；L3 7/10/4=28+40=68；
     *   L4 8/12/4=32+48=80；L5 9/16/5=45+80=125；L6 10/20/6=60+120=180。
     */
    val specs: Map<Level, LevelSpec> = listOf(
        LevelSpec(Level.L1, holdSec = 3, restSec = 6, dailyEndurance = 15, fastSec = 1, fastRestSec = 4, dailyFast = 15, fullSessions = 3),
        LevelSpec(Level.L2, holdSec = 4, restSec = 6, dailyEndurance = 18, fastSec = 1, fastRestSec = 2, dailyFast = 24, fullSessions = 3),
        LevelSpec(Level.L3, holdSec = 5, restSec = 5, dailyEndurance = 28, fastSec = 1, fastRestSec = 2, dailyFast = 40, fullSessions = 4),
        LevelSpec(Level.L4, holdSec = 6, restSec = 6, dailyEndurance = 32, fastSec = 1, fastRestSec = 2, dailyFast = 48, fullSessions = 4),
        LevelSpec(Level.L5, holdSec = 8, restSec = 8, dailyEndurance = 45, fastSec = 1, fastRestSec = 2, dailyFast = 80, fullSessions = 5),
        LevelSpec(Level.L6, holdSec = 10, restSec = 10, dailyEndurance = 60, fastSec = 1, fastRestSec = 2, dailyFast = 120, fullSessions = 6),
    ).associateBy { it.level }

    /** 升级建议阈值（保持秒 / 耐力次数 / 快速次数），沿用已签收数值。 */
    val thresholds: Map<Level, LevelThreshold> = listOf(
        LevelThreshold(Level.L1, holdSec = 3, enduranceReps = 5, fastReps = 5),
        LevelThreshold(Level.L2, holdSec = 4, enduranceReps = 6, fastReps = 6),
        LevelThreshold(Level.L3, holdSec = 5, enduranceReps = 8, fastReps = 8),
        LevelThreshold(Level.L4, holdSec = 6, enduranceReps = 8, fastReps = 10),
        LevelThreshold(Level.L5, holdSec = 8, enduranceReps = 10, fastReps = 10),
        LevelThreshold(Level.L6, holdSec = 10, enduranceReps = 10, fastReps = 10),
    ).associateBy { it.level }

    /** 日额硬上限（产品计数上限，非医学安全保证）。 */
    const val DAILY_LIMIT = 240

    /** 单会话上限（次收缩）。 */
    const val SESSION_MAX = 40

    /** 每耐力组上限（次收缩）。 */
    const val GROUP_MAX = 10

    /** 每快速回合上限（次收缩）。 */
    const val FAST_GROUP_MAX = 20

    /** 所有小组之间（含耐力→快速）完全放松秒数，可延长不可缩短。 */
    const val GROUP_REST_SEC = 30

    /** 准备/结束各 3 秒。 */
    const val READY_SEC = 3
    const val DONE_SEC = 3

    /** 连续完成 6 个训练日后提示休息/半量。 */
    const val REST_DAY_AFTER_CONSECUTIVE_DAYS = 6

    /** 连续 3 个高量日（实际已开始 >120）后提示休息/半量。 */
    const val HIGH_DAY_THRESHOLD = 120
    const val HIGH_DAY_CONSECUTIVE = 3

    /** 距上次摸底满 14 个完成日后提示一次重测。 */
    const val RETEST_PROMPT_AFTER_DAYS = 14

    /** 训练会话次数可选项。 */
    val sessionOptions: List<Int> = listOf(1, 2, 3, 4, 5, 6)

    /** 提醒次数可选项。 */
    val reminderOptions: List<Int> = listOf(1, 2, 3)

    /** 会话次数「自动」哨兵值：按等级默认（3/3/4/4/5/6）。 */
    const val SESSIONS_AUTO = 0

    fun spec(level: Level): LevelSpec = specs.getValue(level)

    /** 满量默认会话数（V2.2：3/3/4/4/5/6）。 */
    fun defaultSessions(level: Level): Int = spec(level).fullSessions

    /** 解析会话次数：非法/自动值回退到该等级默认。 */
    fun resolveSessions(level: Level, chosen: Int): Int =
        if (chosen in sessionOptions) chosen else defaultSessions(level)

    /** 拆分小组数：每同类组 ≤ max，g=ceil(count/max)。 */
    fun groups(count: Int, max: Int = GROUP_MAX): Int =
        if (count <= 0) 0 else (count + max - 1) / max

    /** 快速回合数：每回合 ≤ FAST_GROUP_MAX(20)。 */
    fun fastGroups(count: Int): Int = groups(count, FAST_GROUP_MAX)

    /**
     * 单次会话估算时长（秒）：
     *  T = 6 + e×(H+R) + f×(fastSec+r) + 30×(gE+gF−1)
     *  gE=ceil(e/10)、gF=ceil(f/20)；组间/跨类 30 秒完全放松。
     */
    fun sessionSeconds(e: Int, f: Int, spec: LevelSpec): Int {
        val base = READY_SEC + DONE_SEC
        if (e <= 0 && f <= 0) return base
        val groups = groups(e, GROUP_MAX) + fastGroups(f) - 1
        return base +
            e * (spec.holdSec + spec.restSec) +
            f * (spec.fastSec + spec.fastRestSec) +
            GROUP_REST_SEC * groups.coerceAtLeast(0)
    }

    /** 满量均分会话的单次 e/f（用于「各级标准」展示，按默认会话数）。 */
    fun standardSessionEF(level: Level): Pair<Int, Int> {
        val s = spec(level)
        val n = s.fullSessions
        return (s.dailyEndurance / n) to (s.dailyFast / n)
    }

    /** 「各级标准」单次时长（默认会话数下）。 */
    fun standardSessionSeconds(level: Level): Int {
        val (e, f) = standardSessionEF(level)
        return sessionSeconds(e, f, spec(level))
    }

    /** 满量每日训练秒 = N × 单次时长。 */
    fun standardDailySeconds(level: Level): Int = standardSessionSeconds(level) * spec(level).fullSessions
}
