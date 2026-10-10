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
 */
data class Step(
    val phase: Phase,
    val durationMs: Long,
    val section: Section,
    val repIndex: Int? = null,
    val repTotal: Int? = null,
    val lead: CueId? = null,
    val tails: List<CueAt> = emptyList(),
    val countdown: Boolean = false,
)

/** 训练态：UI 与服务的统一状态 */
enum class TrainingStage { IDLE, RUNNING, PAUSED, FINISHED, SAFETY_STOPPED }

/** 暂停原因（恢复必须由用户显式点「继续」，不自动发声） */
enum class PauseReason { NONE, MANUAL, FOCUS_LOST, NOISY, SCREEN_OFF }

/** 安全出口分类（落库分别记） */
enum class SafetyReason { PAIN, URINARY, RELAX_FAIL }

/**
 * 一次会话的可执行计划（已按等级矩阵、频率降量、半量与当日剩余额度计算完毕）。
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
    val frequencyReduced: Boolean,
    val quotaCapped: Boolean,
) {
    val totalContractions: Int get() = enduranceReps + fastReps
}

sealed interface PlanResult {
    data class Ok(val plan: SessionPlan) : PlanResult
    /** 当日剩余额度不足 2 次收缩，不启动组合训练 */
    data class Blocked(val remaining: Int) : PlanResult
}

object Quota {

    /**
     * 计算一次训练的量：
     *  - 基准取等级矩阵
     *  - 频率 3 → E=min(E,6)，F=min(F,13-E)
     *  - 半量 → 各自 floor(n/2) 且 ≥1
     *  - 与当日剩余额度取小（先减快速，再减耐力，各不低于 1）
     *  - 当日剩余额度 < 2 → 不启动
     */
    fun build(
        level: Level,
        dailyFrequency: Int,
        halfDose: Boolean,
        remainingToday: Int,
    ): PlanResult {
        val spec = LevelRules.spec(level)
        var e = spec.enduranceReps
        var f = spec.fastReps
        var frequencyReduced = false
        if (dailyFrequency >= 3) {
            e = minOf(e, LevelRules.FREQ3_MAX_ENDURANCE)
            f = minOf(f, LevelRules.FREQ3_MAX_TOTAL - e)
            frequencyReduced = true
        }
        if (halfDose) {
            e = maxOf(1, e / 2)
            f = maxOf(1, f / 2)
        }
        if (remainingToday < 2) return PlanResult.Blocked(remainingToday)

        var total = e + f
        var capped = false
        while (total > remainingToday) {
            if (f > 1) f-- else if (e > 1) e-- else break
            total = e + f
            capped = true
        }
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
                frequencyReduced = frequencyReduced,
                quotaCapped = capped,
            )
        )
    }
}

/**
 * 把计划展开成可执行的步骤脚本（纯函数，可单测）。
 * 语音尽量短促、不重叠；快速模式仅「紧 / 松」两字。
 */
object ScriptBuilder {

    fun build(plan: SessionPlan): List<Step> {
        val steps = mutableListOf<Step>()

        // 准备（无「准备等待」阻塞：这是训练内第一步，一键开始即时进入）
        steps += Step(Phase.READY, 3000, Section.ENDURANCE, lead = CueId.READY)

        // 耐力组：收紧 → 保持（末 3 秒倒计时）→ 完全放松
        for (i in 1..plan.enduranceReps) {
            steps += Step(Phase.TIGHTEN, 2000, Section.ENDURANCE, i, plan.enduranceReps, lead = CueId.TIGHTEN)

            val holdMs = plan.holdSec * 1000L
            val holdTails = buildList {
                add(CueAt(CueId.NUM3, holdMs - 3000))
                add(CueAt(CueId.NUM2, holdMs - 2000))
                add(CueAt(CueId.NUM1, holdMs - 1000))
            }.filter { it.atMs >= 0 }.distinctBy { it.cue }
            steps += Step(
                Phase.HOLD, holdMs, Section.ENDURANCE, i, plan.enduranceReps,
                lead = CueId.HOLD, tails = holdTails, countdown = true,
            )

            val relaxMs = plan.restSec * 1000L
            val relaxTails = mutableListOf<CueAt>()
            // 质量提示适度出现一次（FR-AUDIO-03）
            if (i == 1) relaxTails += CueAt(CueId.QUALITY, 1200)
            if (i < plan.enduranceReps) relaxTails += CueAt(CueId.NEXT, maxOf(0, relaxMs - 1500))
            steps += Step(Phase.RELAX, relaxMs, Section.ENDURANCE, i, plan.enduranceReps, lead = CueId.RELAX, tails = relaxTails)
        }

        // 段落间：休息 → 提示进入快速训练
        if (plan.fastReps > 0) {
            val restMs = 5000L
            val tails = if (plan.enduranceReps > 0) listOf(CueAt(CueId.FAST_INTRO, 1500)) else emptyList()
            steps += Step(
                Phase.REST, restMs, Section.REST,
                lead = if (plan.enduranceReps > 0) CueId.REST else CueId.FAST_INTRO,
                tails = tails,
            )
        }

        // 快速组：紧 → 松（短促，不重叠）
        for (j in 1..plan.fastReps) {
            steps += Step(
                Phase.QUICK_TIGHTEN, plan.fastSec * 1000L, Section.FAST, j, plan.fastReps,
                lead = CueId.FAST_TIGHTEN,
            )
            val tails = if (j < plan.fastReps) listOf(CueAt(CueId.FAST_TIGHTEN, maxOf(0, plan.fastRestSec * 1000L)))
            else emptyList()
            steps += Step(
                Phase.QUICK_RELAX, plan.fastRestSec * 1000L, Section.FAST, j, plan.fastReps,
                lead = CueId.FAST_RELAX, tails = tails,
            )
        }

        steps += Step(Phase.DONE, 3000, Section.FINISH, lead = CueId.DONE)
        return steps
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
 */
class SessionScript(val steps: List<Step>) {

    var index: Int = 0
        private set
    var elapsedInStepMs: Long = 0
        private set
    private var firedTails: MutableSet<Int> = mutableSetOf()
    private var started = false

    val totalMs: Long = steps.sumOf { it.durationMs }
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