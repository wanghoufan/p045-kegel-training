package com.orca.kegel.domain

import kotlinx.serialization.Serializable

/**
 * 某本地日内实际已开始的收缩次数（按收缩开始时刻归属本地日，支持跨午夜会话）。
 * 仅用于当日额度归属；不计完成会话、不参与统计。
 */
@Serializable
data class DayUsage(
    val epochDay: Long,
    val enduranceStarted: Int = 0,
    val fastStarted: Int = 0,
)

/** 一次训练会话记录 */
@Serializable
data class SessionRecord(
    val epochDay: Long,
    val timestampMs: Long,
    val level: Level,
    /** 耐力完成次数（完成整轮收紧+放松，用于统计） */
    val enduranceReps: Int,
    /** 有效保持累计秒数（完成次数 × 当前等级 hold 秒） */
    val enduranceHoldSeconds: Int,
    /** 快速完成次数（用于统计） */
    val fastReps: Int,
    /** 会话总时长秒 */
    val durationSeconds: Int,
    /** 是否完整完成（false＝提前结束） */
    val completed: Boolean,
    /** 非空＝由安全出口停止，分类落库 */
    val safetyReason: SafetyReason? = null,
    val halfDose: Boolean = false,
    /** 本次实际已开始的耐力收缩次数（扣当日额度用；中止不返还） */
    val enduranceStarted: Int = 0,
    /** 本次实际已开始的快速收缩次数（扣当日额度用；中止不返还） */
    val fastStarted: Int = 0,
    /**
     * 剂量版本：V1 旧记录无此字段，解码回退 1（可追溯、不受 V2 上限追溯改写）；
     * V2 记录显式写 2。
     */
    val doseVersion: Int = 1,
    /** 按发生日的已开始收缩分桶（跨午夜归属；为空时按 epochDay 回退） */
    val startedByDay: List<DayUsage> = emptyList(),
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

/** 某一天的训练聚合（历史页图表/列表用）。 */
data class DailyStat(
    val epochDay: Long,
    val sessions: Int,
    val durationSeconds: Int,
    val enduranceReps: Int,
    val fastReps: Int,
) {
    val totalReps: Int get() = enduranceReps + fastReps
}

/** 历史页总计。 */
data class HistorySummary(
    val totalSessions: Int,
    val totalDays: Int,
    val totalDurationSeconds: Int,
    val totalEnduranceReps: Int,
    val totalFastReps: Int,
    val totalHoldSeconds: Int,
) {
    val totalReps: Int get() = totalEnduranceReps + totalFastReps
}

/** 历史页聚合（纯函数，可单测）。 */
object HistoryInsights {

    fun summary(records: List<SessionRecord>): HistorySummary =
        HistorySummary(
            totalSessions = records.size,
            totalDays = records.map { it.epochDay }.distinct().size,
            totalDurationSeconds = records.sumOf { it.durationSeconds },
            totalEnduranceReps = records.sumOf { it.enduranceReps },
            totalFastReps = records.sumOf { it.fastReps },
            totalHoldSeconds = records.sumOf { it.enduranceHoldSeconds },
        )

    /** 最近 [days] 天（含无训练日，按日期升序），[todayEpochDay] 为最后一天。 */
    fun daily(records: List<SessionRecord>, days: Int, todayEpochDay: Long): List<DailyStat> {
        if (days <= 0) return emptyList()
        val byDay = records.groupBy { it.epochDay }
        val start = todayEpochDay - (days - 1)
        return (start..todayEpochDay).map { d ->
            val rs = byDay[d].orEmpty()
            DailyStat(
                epochDay = d,
                sessions = rs.size,
                durationSeconds = rs.sumOf { it.durationSeconds },
                enduranceReps = rs.sumOf { it.enduranceReps },
                fastReps = rs.sumOf { it.fastReps },
            )
        }
    }
}

