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
 * 六级单值参数矩阵（用户 2026-10-07 批准，单一真源）。
 * 字段：耐力收紧秒 / 完全放松秒 / 耐力次数 / 快速收紧秒 / 快速间隔秒 / 快速次数
 */
data class LevelSpec(
    val level: Level,
    val holdSec: Int,
    val restSec: Int,
    val enduranceReps: Int,
    val fastSec: Int,
    val fastRestSec: Int,
    val fastReps: Int,
)

/** 初始等级建议阈值（短板约束：三项全部达标才取该级） */
data class LevelThreshold(
    val level: Level,
    val holdSec: Int,
    val enduranceReps: Int,
    val fastReps: Int,
)

object LevelRules {

    val specs: Map<Level, LevelSpec> = listOf(
        LevelSpec(Level.L1, holdSec = 3, restSec = 6, enduranceReps = 5, fastSec = 1, fastRestSec = 4, fastReps = 5),
        LevelSpec(Level.L2, holdSec = 4, restSec = 6, enduranceReps = 6, fastSec = 1, fastRestSec = 3, fastReps = 6),
        LevelSpec(Level.L3, holdSec = 5, restSec = 5, enduranceReps = 8, fastSec = 1, fastRestSec = 3, fastReps = 8),
        LevelSpec(Level.L4, holdSec = 6, restSec = 6, enduranceReps = 8, fastSec = 1, fastRestSec = 2, fastReps = 10),
        LevelSpec(Level.L5, holdSec = 8, restSec = 8, enduranceReps = 10, fastSec = 1, fastRestSec = 2, fastReps = 10),
        LevelSpec(Level.L6, holdSec = 10, restSec = 10, enduranceReps = 10, fastSec = 1, fastRestSec = 2, fastReps = 10),
    ).associateBy { it.level }

    val thresholds: Map<Level, LevelThreshold> = listOf(
        LevelThreshold(Level.L1, holdSec = 3, enduranceReps = 5, fastReps = 5),
        LevelThreshold(Level.L2, holdSec = 4, enduranceReps = 6, fastReps = 6),
        LevelThreshold(Level.L3, holdSec = 5, enduranceReps = 8, fastReps = 8),
        LevelThreshold(Level.L4, holdSec = 6, enduranceReps = 8, fastReps = 10),
        LevelThreshold(Level.L5, holdSec = 8, enduranceReps = 10, fastReps = 10),
        LevelThreshold(Level.L6, holdSec = 10, enduranceReps = 10, fastReps = 10),
    ).associateBy { it.level }

    /** 日上限：每日所有会话合计不超过 40 次收缩 */
    const val DAILY_CONTRACTION_LIMIT = 40

    /** 连续完成 6 日后提示休息/半量 */
    const val REST_DAY_AFTER_CONSECUTIVE_DAYS = 6

    /** 距上次摸底满 14 个完成日后提示一次重测 */
    const val RETEST_PROMPT_AFTER_DAYS = 14

    /** 频率选 3 时的降量上限：E 不超过 6 */
    const val FREQ3_MAX_ENDURANCE = 6

    /** 频率选 3 时的降量上限：E + F 不超过 13 */
    const val FREQ3_MAX_TOTAL = 13

    fun spec(level: Level): LevelSpec = specs.getValue(level)

    /**
     * 单次训练估算时长（秒），用于「各级标准」页展示。
     * 结构：准备3s + 耐力(E×(2s收紧+hold+rest)) + 组间5s + 快速(F×(fast+fastRest)) + 结束3s
     */
    fun estimateSessionSeconds(level: Level): Int {
        val s = spec(level)
        val ready = 3
        val endurance = s.enduranceReps * (2 + s.holdSec + s.restSec)
        val between = 5
        val fast = s.fastReps * (s.fastSec + s.fastRestSec)
        val done = 3
        return ready + endurance + between + fast + done
    }
}