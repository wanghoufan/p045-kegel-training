package com.orca.kegel.domain

import kotlinx.serialization.Serializable

/** 一次训练会话记录 */
@Serializable
data class SessionRecord(
    val epochDay: Long,
    val timestampMs: Long,
    val level: Level,
    /** 耐力完成次数 */
    val enduranceReps: Int,
    /** 有效保持累计秒数（完成次数 × 当前等级 hold 秒） */
    val enduranceHoldSeconds: Int,
    /** 快速完成次数 */
    val fastReps: Int,
    /** 会话总时长秒 */
    val durationSeconds: Int,
    /** 是否完整完成（false＝提前结束） */
    val completed: Boolean,
    /** 非空＝由安全出口停止，分类落库 */
    val safetyReason: SafetyReason? = null,
    val halfDose: Boolean = false,
)

/** 一次等级变化记录（重测不静默覆盖历史） */
@Serializable
data class LevelChange(
    val epochDay: Long,
    val from: Level?,
    val to: Level,
    val reason: String,
)

/** EHS 可选自评记录（独立于训练等级） */
@Serializable
data class EhsRecord(val epochDay: Long, val score: Int)

data class HistoryStats(
    val sessions: Int,
    val days: Int,
    val enduranceRepsTotal: Int,
    val enduranceHoldSecondsTotal: Int,
    val longestHoldSeconds: Int,
    val fastRepsTotal: Int,
) {
    companion object {
        fun of(records: List<SessionRecord>): HistoryStats {
            val valid = records.filter { it.safetyReason == null }
            return HistoryStats(
                sessions = records.size,
                days = records.map { it.epochDay }.distinct().size,
                enduranceRepsTotal = records.sumOf { it.enduranceReps },
                enduranceHoldSecondsTotal = records.sumOf { it.enduranceHoldSeconds },
                longestHoldSeconds = records.maxOfOrNull { it.enduranceHoldSeconds / maxOf(1, it.enduranceReps) } ?: 0,
                fastRepsTotal = records.sumOf { it.fastReps },
            )
        }
    }
}

