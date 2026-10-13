package com.orca.kegel.domain

/** 训练阶段（语音/字幕相位） */
enum class Phase {
    READY, TIGHTEN, HOLD, RELAX, REST, NEXT, QUICK_TIGHTEN, QUICK_RELAX, DONE
}

/** 所属训练段落 */
enum class Section { ENDURANCE, REST, FAST, FINISH }

/** 离线语音提示条 ID（映射到 res/raw 资产，按当前语言解析） */
enum class CueId {
    READY, TIGHTEN, HOLD, RELAX, REST, NEXT,
    FAST_TIGHTEN, FAST_RELAX, DONE, FAST_INTRO, QUALITY,
    NUM3, NUM2, NUM1
}

data class CueAt(val cue: CueId, val atMs: Long)

/**
 * 一个训练步骤。时长以毫秒计，lead 在步骤开始时播报，tails 在步骤内指定偏移播报。
 * durationMs 可被「延长休息」在运行中增大（不可缩短）。
 */
data class Step(
    val phase: Phase,
    var durationMs: Long,
    val section: Section,
    val repIndex: Int? = null,
    val repTotal: Int? = null,
    val lead: CueId? = null,
    val tails: List<CueAt> = emptyList(),
    val countdown: Boolean = false,
    val groupIndex: Int? = null,
    val groupTotal: Int? = null,
)

/** 训练态：UI 与服务的统一状态 */
enum class TrainingStage { IDLE, RUNNING, PAUSED, FINISHED, SAFETY_STOPPED }

/** 暂停原因（恢复必须由用户显式点「继续」，不自动发声） */
enum class PauseReason { NONE, MANUAL, FOCUS_LOST, NOISY, SCREEN_OFF }

/** 安全出口分类（落库分别记） */
enum class SafetyReason { PAIN, URINARY, RELAX_FAIL, FATIGUE }

/**
 * 一次会话的可执行计划（已按等级矩阵、会话次数、半量与当日本类已用额度计算完毕）。
 *  - enduranceReps/fastReps 为本次可执行次数
 *  - sessionIndex 为本次是当日第几个时段（1..sessionCount）
 *  - reducedByRemaining＝因当日余量不足被下调
 */
data class SessionPlan(
    val level: Level,
    val holdSec: Int,
    val restSec: Int,
    val enduranceReps: Int,
    val fastSec: Int,
    val fastRestSec: Int,
    val fastReps: Int,
    val halfDose: Boolean,
    val reducedByRemaining: Boolean,
    val sessionIndex: Int,
    val sessionCount: Int,
    val doseVersion: Int = DOSE_VERSION,
) {
    val totalContractions: Int get() = enduranceReps + fastReps
    val enduranceGroups: Int get() = LevelRules.groups(enduranceReps)
    val fastGroups: Int get() = LevelRules.fastGroups(fastReps)
    val totalGroups: Int get() = enduranceGroups + fastGroups
    val estimatedSeconds: Int get() = LevelRules.sessionSeconds(enduranceReps, fastReps, LevelRules.spec(level))

    companion object {
        const val DOSE_VERSION = 2
    }
}

sealed interface PlanResult {
    data class Ok(val plan: SessionPlan) : PlanResult

    /** 当日额度或时段已用尽，不启动 */
    data class Blocked(val remainingE: Int, val remainingF: Int) : PlanResult
}

/** 当日冻结额度（按类型拆分）。 */
data class DailyAllocation(
    val sessions: Int,
    val eQuota: Int,
    val fQuota: Int,
) {
    val total: Int get() = eQuota + fQuota
}

object Quota {

    /**
     * 当日额度（确定性换算，V2.2 FR-005）：
     *  每次会话固定 e/q（spec.endurancePerSession / fastPerSession），日额 = 每次 × N。
     *  会话数越少日总量按比例越少（每次会话量不变，保证每次一致，无余数分摊）。
     *  半量：E_H=floor(E/2)、F_H=floor(F/2)（非零时至少 1 次）。
     *  日总受 DAILY_LIMIT(240) 保护。
     */
    fun daily(level: Level, sessions: Int, halfDose: Boolean): DailyAllocation {
        val spec = LevelRules.spec(level)
        val n = sessions.coerceAtLeast(1)
        var e = spec.endurancePerSession * n
        var f = spec.fastPerSession * n
        if (halfDose) {
            e = halfFloor(e)
            f = halfFloor(f)
        }
        // 日上限保护（当前最高 180，保留以防调参越界）
        val cap = LevelRules.DAILY_LIMIT
        if (e + f > cap && e + f > 0) {
            val scaledE = cap * e / (e + f)
            f = cap - scaledE
            e = scaledE
        }
        return DailyAllocation(sessions = n, eQuota = e, fQuota = f)
    }

    /** 半量折半：向下取整为一半，非零时至少 1 次。 */
    private fun halfFloor(v: Int): Int = if (v <= 0) 0 else maxOf(1, v / 2)

    /**
     * 第 index 个时段的原始分配（1-based）：
     *  q_i = floor(Q/N) + (i ≤ Q mod N ? 1 : 0)
     *  c_i = 前 i 个 q 之和；e_i = floor(c_i×E_Q/Q) − floor(c_{i−1}×E_Q/Q)；f_i = q_i − e_i
     *  index 超出时段数或 Q=0 → 0/0。
     */
    fun forSession(alloc: DailyAllocation, index: Int): Pair<Int, Int> {
        val q = alloc.total
        if (q <= 0 || index < 1 || index > alloc.sessions) return 0 to 0
        val n = alloc.sessions
        fun slot(i: Int): Int = q / n + if (i <= q % n) 1 else 0

        var c = 0
        for (i in 1..index) c += slot(i)
        val cPrev = c - slot(index)
        val ei = c * alloc.eQuota / q - cPrev * alloc.eQuota / q
        val fi = slot(index) - ei
        return ei to fi
    }

    /**
     * 计算本次训练量：
     *  - 取当日冻结额度与该时段原始分配
     *  - 读当日实际已开始 E/F，本次 e=min(e_i, rE)、f=min(f_i, rF)
     *  - 两类各至少 1；不足则退化为余量专项；两类都 <1 则不启动
     *  - 不先扣快速把额度转耐力（严格按类型余量取小）
     */
    fun build(
        level: Level,
        sessions: Int,
        halfDose: Boolean,
        sessionIndex: Int,
        usedE: Int,
        usedF: Int,
    ): PlanResult {
        val spec = LevelRules.spec(level)
        val n = sessions.coerceAtLeast(1)
        val alloc = daily(level, n, halfDose)
        val rE = (alloc.eQuota - usedE).coerceAtLeast(0)
        val rF = (alloc.fQuota - usedF).coerceAtLeast(0)
        if (rE < 1 && rF < 1) return PlanResult.Blocked(rE, rF)

        val (eI, fI) = forSession(alloc, sessionIndex)
        val e = minOf(eI, rE)
        val f = minOf(fI, rF)
        if (e < 1 && f < 1) return PlanResult.Blocked(rE, rF)

        val reduced = e < eI || f < fI
        return PlanResult.Ok(
            SessionPlan(
                level = level,
                holdSec = spec.holdSec,
                restSec = spec.restSec,
                enduranceReps = e,
                fastSec = spec.fastSec,
                fastRestSec = spec.fastRestSec,
                fastReps = f,
                halfDose = halfDose,
                reducedByRemaining = reduced,
                sessionIndex = sessionIndex,
                sessionCount = n,
            )
        )
    }
}

/**
 * 把计划展开成可执行的步骤脚本（纯函数，可单测）。
 *  - 单次保持 H 含收紧起始（不再另加 2 秒）
 *  - 耐力每组 ≤10 次、快速每回合 ≤20 次，组间与跨类之间 30 秒完全放松
 *  - 语音尽量短促、不重叠；快速模式仅「紧 / 松」两字
 */
object ScriptBuilder {

    fun build(plan: SessionPlan): List<Step> {
        val steps = mutableListOf<Step>()

        val eGroups = LevelRules.groups(plan.enduranceReps)
        val fGroups = LevelRules.fastGroups(plan.fastReps)
        val totalGroups = eGroups + fGroups

        // 准备：若本次只有快速（无耐力），在准备阶段顺带提示进入快速
        val readyTails = if (plan.enduranceReps <= 0 && plan.fastReps > 0) {
            listOf(CueAt(CueId.FAST_INTRO, 1500))
        } else emptyList()
        steps += Step(Phase.READY, LevelRules.READY_SEC * 1000L, Section.ENDURANCE, lead = CueId.READY, tails = readyTails)

        var groupSeq = 0

        // 耐力：每小组 ≤10 次；HOLD 含收紧起始；组间 30 秒
        for (g in 0 until eGroups) {
            val from = g * LevelRules.GROUP_MAX
            val to = minOf(plan.enduranceReps, from + LevelRules.GROUP_MAX)
            for (i in from until to) {
                val repNo = i + 1
                val holdMs = plan.holdSec * 1000L
                val holdTails = buildList {
                    // H 含收紧起始：先「收紧」，稍后补一声「保持」；末 3 秒 3/2/1
                    if (plan.holdSec >= 5) add(CueAt(CueId.HOLD, 1000L))
                    add(CueAt(CueId.NUM3, holdMs - 3000))
                    add(CueAt(CueId.NUM2, holdMs - 2000))
                    add(CueAt(CueId.NUM1, holdMs - 1000))
                }.filter { it.atMs >= 0 }.distinctBy { it.cue }.sortedBy { it.atMs }
                steps += Step(
                    Phase.HOLD, holdMs, Section.ENDURANCE, repNo, plan.enduranceReps,
                    lead = CueId.TIGHTEN, tails = holdTails, countdown = true,
                    groupIndex = g + 1, groupTotal = eGroups,
                )

                val relaxMs = plan.restSec * 1000L
                val relaxTails = mutableListOf<CueAt>()
                if (repNo == 1) relaxTails += CueAt(CueId.QUALITY, 1200)
                // 组内还有下一次 → 放松末尾提示「下一次」；组末由组间休息的提示承接
                if (repNo < to) relaxTails += CueAt(CueId.NEXT, maxOf(0, relaxMs - 1500))
                steps += Step(
                    Phase.RELAX, relaxMs, Section.ENDURANCE, repNo, plan.enduranceReps,
                    lead = CueId.RELAX, tails = relaxTails,
                    groupIndex = g + 1, groupTotal = eGroups,
                )
            }
            groupSeq++
            if (groupSeq < totalGroups) {
                val sectionBoundary = g == eGroups - 1 && fGroups > 0
                steps += restStep(
                    index = groupSeq,
                    total = totalGroups,
                    lead = CueId.REST,
                    tailCue = if (sectionBoundary) CueId.FAST_INTRO else CueId.NEXT,
                )
            }
        }

        // 快速：每回合 ≤20 次；组间 30 秒
        for (g in 0 until fGroups) {
            val from = g * LevelRules.FAST_GROUP_MAX
            val to = minOf(plan.fastReps, from + LevelRules.FAST_GROUP_MAX)
            for (j in from until to) {
                val repNo = j + 1
                steps += Step(
                    Phase.QUICK_TIGHTEN, plan.fastSec * 1000L, Section.FAST, repNo, plan.fastReps,
                    lead = CueId.FAST_TIGHTEN, groupIndex = g + 1, groupTotal = fGroups,
                )
                val tails = if (repNo < to) {
                    listOf(CueAt(CueId.FAST_TIGHTEN, plan.fastRestSec * 1000L))
                } else emptyList()
                steps += Step(
                    Phase.QUICK_RELAX, plan.fastRestSec * 1000L, Section.FAST, repNo, plan.fastReps,
                    lead = CueId.FAST_RELAX, tails = tails,
                    groupIndex = g + 1, groupTotal = fGroups,
                )
            }
            groupSeq++
            if (groupSeq < totalGroups) {
                steps += restStep(
                    index = groupSeq,
                    total = totalGroups,
                    lead = CueId.REST,
                    tailCue = CueId.NEXT,
                )
            }
        }

        steps += Step(Phase.DONE, LevelRules.DONE_SEC * 1000L, Section.FINISH, lead = CueId.DONE)
        return steps
    }

    private fun restStep(index: Int, total: Int, lead: CueId, tailCue: CueId): Step {
        val restMs = LevelRules.GROUP_REST_SEC * 1000L
        return Step(
            Phase.REST, restMs, Section.REST,
            lead = lead,
            tails = listOf(CueAt(tailCue, maxOf(0, restMs - 1500))),
            groupIndex = index, groupTotal = total,
            countdown = true,
        )
    }
}

data class TickResult(
    val cues: List<CueId>,
    val stepChanged: Boolean,
    val finished: Boolean,
    val completed: List<Step> = emptyList(),
)

/**
 * 会话推进器：以 deltaMs 推进，返回本 tick 需要播报的 cue 与是否结束/换步。
 * 计时器是主时钟（语音只是提示，不让语音延迟拖节奏）。
 * 组间休息可被「延长休息」在运行中加长（不可缩短）。
 */
class SessionScript(val steps: List<Step>) {

    var index: Int = 0
        private set
    var elapsedInStepMs: Long = 0
        private set
    private var firedTails: MutableSet<Int> = mutableSetOf()
    private var started = false

    val totalMs: Long get() = steps.sumOf { it.durationMs }
    val isFinished: Boolean get() = index >= steps.size
    fun current(): Step? = steps.getOrNull(index)

    /** 已完成的训练时长（毫秒） */
    fun elapsedTotalMs(): Long =
        steps.take(index).sumOf { it.durationMs } + elapsedInStepMs

    /** 开始：返回第一步的 lead cue */
    fun start(): CueId? {
        started = true
        index = 0
        elapsedInStepMs = 0
        firedTails = mutableSetOf()
        return steps.firstOrNull()?.lead
    }

    /**
     * 延长当前步骤（仅用于组间休息，可延长不可缩短）。
     * 返回是否实际延长。
     */
    fun extendCurrent(extraMs: Long): Boolean {
        if (extraMs <= 0) return false
        val step = current() ?: return false
        if (step.phase != Phase.REST) return false
        step.durationMs += extraMs
        return true
    }

    fun tick(deltaMs: Long): TickResult {
        if (!started || isFinished) return TickResult(emptyList(), false, isFinished)
        val cues = mutableListOf<CueId>()
        val completed = mutableListOf<Step>()
        var stepChanged = false
        var remaining = deltaMs
        while (remaining > 0 && !isFinished) {
            val step = steps[index]
            val stepRemaining = step.durationMs - elapsedInStepMs
            if (stepRemaining <= 0) {
                // 进入下一步（当前步计为已完成）
                completed += step
                index++
                elapsedInStepMs = 0
                firedTails = mutableSetOf()
                stepChanged = true
                val nextStep = steps.getOrNull(index)
                if (nextStep == null) {
                    return TickResult(cues, stepChanged, true, completed)
                }
                nextStep.lead?.let { cues += it }
                // 处理新步骤在 0 偏移的 tail
                fireTails(nextStep, cues)
                continue
            }
            val consume = minOf(remaining, stepRemaining)
            elapsedInStepMs += consume
            remaining -= consume
            fireTails(step, cues)
            if (elapsedInStepMs >= step.durationMs) {
                // 交给下一轮循环推进
                continue
            }
        }
        return TickResult(cues, stepChanged, isFinished, completed)
    }

    private fun fireTails(step: Step, out: MutableList<CueId>) {
        step.tails.forEachIndexed { i, tail ->
            if (i !in firedTails && elapsedInStepMs >= tail.atMs) {
                firedTails += i
                out += tail.cue
            }
        }
    }
}
