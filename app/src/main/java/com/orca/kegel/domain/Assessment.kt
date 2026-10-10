package com.orca.kegel.domain

import kotlinx.serialization.Serializable

/** 动作控制质量（摸底第 1 项） */
enum class ControlQuality { CLEAR, UNSURE, COMPENSATING }

/** 放松质量（摸底第 5 项） */
enum class RelaxQuality { GOOD, UNCERTAIN, POOR }

/**
 * 一次摸底的结果。仅作默认建议，不锁等级，不覆盖历史记录。
 */
@Serializable
data class AssessmentResult(
    val epochDay: Long,
    val controlQuality: ControlQuality,
    /** 有效耐力保持秒数，1–10 */
    val holdSeconds: Int,
    /** 耐力重复次数，1–10 */
    val enduranceReps: Int,
    /** 快速收缩次数，1–10 */
    val fastReps: Int,
    val relaxQuality: RelaxQuality,
) {
    companion object {
        const val MAX_HOLD_SECONDS = 10
        const val MAX_REPS = 10
    }
}

object Recommender {

    /**
     * 短板约束 + 一票否决：
     *  - 动作控制明显依赖其他肌群（COMPENSATING）→ 不做等级建议（null）
     *  - 放松质量差（POOR）→ 不做等级建议（null）
     *  - 否则从 L6 往下取第一个「耐力秒 / 耐力次数 / 快速次数」三项全部达标的等级
     *  - 都不达标时给最低等级 L1（至少 L1 开始）
     */
    fun recommend(result: AssessmentResult): Level? {
        if (result.controlQuality == ControlQuality.COMPENSATING) return null
        if (result.relaxQuality == RelaxQuality.POOR) return null
        for (level in Level.entries.reversed()) {
            val t = LevelRules.thresholds.getValue(level)
            if (result.holdSeconds >= t.holdSec &&
                result.enduranceReps >= t.enduranceReps &&
                result.fastReps >= t.fastReps
            ) return level
        }
        return Level.L1
    }

    /**
     * 升级建议：需要多维度共同达标（耐力秒 / 耐力次数 / 快速次数 + 控制与放松质量正常）。
     * 不因单一维度超强而升级。
     */
    fun suggestUpgrade(current: Level, result: AssessmentResult): Level? {
        if (result.controlQuality == ControlQuality.COMPENSATING) return null
        if (result.relaxQuality == RelaxQuality.POOR) return null
        val next = Level.entries.firstOrNull { it.num == current.num + 1 } ?: return null
        val t = LevelRules.thresholds.getValue(next)
        val meets = result.holdSeconds >= t.holdSec &&
            result.enduranceReps >= t.enduranceReps &&
            result.fastReps >= t.fastReps
        return if (meets) next else null
    }
}