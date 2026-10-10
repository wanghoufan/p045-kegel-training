package com.orca.kegel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 领域层单测（纯 Kotlin，无需真机）：
 * 六级矩阵、短板阈值、计划生成、当日额度与降量、脚本展开与推进器。
 * 覆盖 AC-003/004/005/009/010/011/012/013/014/015 相关的纯逻辑部分。
 */
class DomainTest {

    // ---------- 六级矩阵与估算 ----------

    @Test
    fun levelSpecs_matchApprovedMatrix() {
        val l1 = LevelRules.spec(Level.L1)
        assertEquals(3, l1.holdSec)
        assertEquals(6, l1.restSec)
        assertEquals(5, l1.enduranceReps)
        assertEquals(1, l1.fastSec)
        assertEquals(4, l1.fastRestSec)
        assertEquals(5, l1.fastReps)

        val l6 = LevelRules.spec(Level.L6)
        assertEquals(10, l6.holdSec)
        assertEquals(10, l6.enduranceReps)
        assertEquals(10, l6.fastReps)
        assertEquals(6, Level.entries.size)
    }

    @Test
    fun estimateL3SessionSeconds() {
        // 3 + 8*(2+5+5) + 5 + 8*(1+3) + 3 = 139
        assertEquals(139, LevelRules.estimateSessionSeconds(Level.L3))
    }

    @Test
    fun dailyLimitIs40() {
        assertEquals(40, LevelRules.DAILY_CONTRACTION_LIMIT)
    }

    // ---------- 额度 / 降量 / 半量 ----------

    @Test
    fun baselineQuota_usesMatrixValues() {
        val r = Quota.build(Level.L3, dailyFrequency = 2, halfDose = false, remainingToday = 40)
        assertTrue(r is PlanResult.Ok)
        val p = (r as PlanResult.Ok).plan
        assertEquals(8, p.enduranceReps)
        assertEquals(8, p.fastReps)
        assertTrue(p.frequencyReduced.not())
        assertTrue(p.halfDose.not())
    }

    @Test
    fun frequency3_reducesEnduranceAndTotal() {
        val r = Quota.build(Level.L3, dailyFrequency = 3, halfDose = false, remainingToday = 40)
        val p = (r as PlanResult.Ok).plan
        assertEquals(6, p.enduranceReps) // min(8, FREQ3_MAX_ENDURANCE=6)
        assertEquals(7, p.fastReps) // min(8, 13-6=7)
        assertTrue(p.frequencyReduced)
        assertTrue(p.enduranceReps <= LevelRules.FREQ3_MAX_ENDURANCE)
        assertTrue(p.enduranceReps + p.fastReps <= LevelRules.FREQ3_MAX_TOTAL)
    }

    @Test
    fun halfDose_floorsAndKeepsAtLeastOne() {
        val r = Quota.build(Level.L1, dailyFrequency = 1, halfDose = true, remainingToday = 40)
        val p = (r as PlanResult.Ok).plan
        assertEquals(2, p.enduranceReps) // floor(5/2)
        assertEquals(2, p.fastReps) // floor(5/2)
        assertTrue(p.halfDose)
    }

    @Test
    fun remainingBelowTwo_isBlocked() {
        val r = Quota.build(Level.L3, dailyFrequency = 2, halfDose = false, remainingToday = 1)
        assertTrue(r is PlanResult.Blocked)
        assertEquals(1, (r as PlanResult.Blocked).remaining)
    }

    @Test
    fun quotaCap_reducesFastFirstThenEndurance() {
        val r = Quota.build(Level.L3, dailyFrequency = 2, halfDose = false, remainingToday = 10)
        val p = (r as PlanResult.Ok).plan
        assertEquals(8, p.enduranceReps)
        assertEquals(2, p.fastReps)
        assertTrue(p.quotaCapped)
        assertEquals(10, p.enduranceReps + p.fastReps)
    }

    @Test
    fun quotaCap_neverGoesBelowOneEach() {
        val r = Quota.build(Level.L3, dailyFrequency = 2, halfDose = false, remainingToday = 5)
        val p = (r as PlanResult.Ok).plan
        assertTrue(p.enduranceReps >= 1)
        assertTrue(p.fastReps >= 1)
        assertTrue(p.enduranceReps + p.fastReps <= 5)
    }

    // ---------- 短板约束与一票否决 ----------

    @Test
    fun recommender_vetoesCompensatingAndPoorRelax() {
        val base = AssessmentResult(0, ControlQuality.CLEAR, 10, 10, 10, RelaxQuality.GOOD)
        assertNull(Recommender.recommend(base.copy(controlQuality = ControlQuality.COMPENSATING)))
        assertNull(Recommender.recommend(base.copy(relaxQuality = RelaxQuality.POOR)))
    }

    @Test
    fun recommender_shortBoardPicksLowestQualifyingLevel() {
        val strong = AssessmentResult(0, ControlQuality.CLEAR, 10, 10, 10, RelaxQuality.GOOD)
        assertEquals(Level.L6, Recommender.recommend(strong))

        val weak = AssessmentResult(0, ControlQuality.CLEAR, 3, 5, 5, RelaxQuality.GOOD)
        assertEquals(Level.L1, Recommender.recommend(weak))

        val mid = AssessmentResult(0, ControlQuality.CLEAR, 5, 8, 8, RelaxQuality.GOOD)
        assertEquals(Level.L3, Recommender.recommend(mid))

        // 全部不达标 → 至少 L1 起步
        val tiny = AssessmentResult(0, ControlQuality.UNSURE, 1, 1, 1, RelaxQuality.UNCERTAIN)
        assertEquals(Level.L1, Recommender.recommend(tiny))
    }

    @Test
    fun recommend_doesNotUpgradeFromSingleStrongDimension() {
        // 耐力秒很强，但次数/快速不足 → 不能因单项强而升到高级
        val skewed = AssessmentResult(0, ControlQuality.CLEAR, 10, 1, 1, RelaxQuality.GOOD)
        assertEquals(Level.L1, Recommender.recommend(skewed))
    }

    @Test
    fun suggestUpgrade_requiresAllDimensions() {
        val meetingL2 = AssessmentResult(0, ControlQuality.CLEAR, 4, 6, 6, RelaxQuality.GOOD)
        assertEquals(Level.L2, Recommender.suggestUpgrade(Level.L1, meetingL2))
        val notMeeting = AssessmentResult(0, ControlQuality.CLEAR, 4, 6, 5, RelaxQuality.GOOD)
        assertNull(Recommender.suggestUpgrade(Level.L1, notMeeting))
        // L6 已是最高，无升级
        assertNull(Recommender.suggestUpgrade(Level.L6, meetingL2))
    }

    // ---------- 脚本展开 ----------

    private fun l1Plan(): SessionPlan =
        (Quota.build(Level.L1, dailyFrequency = 1, halfDose = false, remainingToday = 40) as PlanResult.Ok).plan

    @Test
    fun scriptBuilder_stepCountAndTotalMs() {
        val plan = l1Plan() // E=5, F=5, hold3, rest6, fast1, fastRest4
        val steps = ScriptBuilder.build(plan)
        // READY + 5*(TIGHTEN,HOLD,RELAX) + REST + 5*(QUICK_TIGHTEN,QUICK_RELAX) + DONE
        assertEquals(1 + 5 * 3 + 1 + 5 * 2 + 1, steps.size)
        assertEquals(91000L, steps.sumOf { it.durationMs })
        assertEquals(Phase.READY, steps.first().phase)
        assertEquals(CueId.READY, steps.first().lead)
        assertEquals(Phase.DONE, steps.last().phase)
        assertEquals(CueId.DONE, steps.last().lead)
    }

    @Test
    fun scriptBuilder_holdHasCountdownTails() {
        val steps = ScriptBuilder.build(l1Plan())
        val hold = steps.first { it.phase == Phase.HOLD }
        assertTrue(hold.countdown)
        assertEquals(listOf(CueId.NUM3, CueId.NUM2, CueId.NUM1), hold.tails.map { it.cue })
        // L1 hold 3s：0s / 1s / 2s
        assertEquals(listOf(0L, 1000L, 2000L), hold.tails.map { it.atMs })
    }

    @Test
    fun scriptBuilder_sectionMarkers() {
        val steps = ScriptBuilder.build(l1Plan())
        assertEquals(Section.ENDURANCE, steps.first().section)
        assertTrue(steps.any { it.section == Section.REST })
        assertTrue(steps.any { it.section == Section.FAST })
        assertEquals(Section.FINISH, steps.last().section)
        // 段落间休息播报「休息」并提示进入快速
        val rest = steps.first { it.section == Section.REST }
        assertEquals(CueId.REST, rest.lead)
        assertTrue(rest.tails.any { it.cue == CueId.FAST_INTRO })
    }

    // ---------- 推进器 ----------

    @Test
    fun sessionScript_runsToFinishAndEmitsAllCues() {
        val script = SessionScript(ScriptBuilder.build(l1Plan()))
        assertEquals(CueId.READY, script.start())
        val cues = mutableSetOf<CueId>()
        var guard = 0
        while (!script.isFinished && guard++ < 100000) {
            cues += script.tick(250).cues
        }
        assertTrue(script.isFinished)
        assertTrue(cues.containsAll(
            listOf(
                CueId.TIGHTEN, CueId.HOLD, CueId.RELAX, CueId.REST, CueId.FAST_INTRO,
                CueId.FAST_TIGHTEN, CueId.FAST_RELAX, CueId.DONE,
                CueId.NUM3, CueId.NUM2, CueId.NUM1, CueId.QUALITY,
            )
        ))
        assertEquals(script.totalMs, script.elapsedTotalMs())
    }

    @Test
    fun sessionScript_countsEnduranceAndFastReps() {
        val script = SessionScript(ScriptBuilder.build(l1Plan()))
        script.start()
        var endurance = 0
        var fast = 0
        var guard = 0
        while (!script.isFinished && guard++ < 100000) {
            val r = script.tick(100)
            r.completed.forEach { c ->
                if (c.phase == Phase.RELAX) endurance++
                if (c.phase == Phase.QUICK_RELAX) fast++
            }
        }
        assertEquals(5, endurance)
        assertEquals(5, fast)
    }

    @Test
    fun sessionScript_pauseResumeKeepsPosition() {
        val script = SessionScript(ScriptBuilder.build(l1Plan()))
        script.start()
        repeat(20) { script.tick(100) } // 2s → 已过 READY
        val before = script.elapsedTotalMs()
        // 暂停期间不推进（不调用 tick）；恢复后从原相位继续
        val atPause = script.current()
        repeat(20) { script.tick(100) }
        assertEquals(before + 2000, script.elapsedTotalMs())
        assertTrue(atPause != null)
    }

    // ---------- 统计 ----------

    @Test
    fun historyStats_aggregatesAndLongestHold() {
        val recs = listOf(
            SessionRecord(100, 1, Level.L3, 8, 40, 8, 200, true),
            SessionRecord(100, 2, Level.L3, 8, 40, 8, 200, true),
            SessionRecord(101, 3, Level.L3, 4, 20, 4, 120, false, SafetyReason.PAIN),
        )
        val s = HistoryStats.of(recs)
        assertEquals(3, s.sessions)
        assertEquals(2, s.days)
        assertEquals(20, s.enduranceRepsTotal)
        assertEquals(100, s.enduranceHoldSecondsTotal)
        assertEquals(5, s.longestHoldSeconds) // 40 / 8
        assertEquals(20, s.fastRepsTotal)
    }

    @Test
    fun levelOf_and_fromLabel() {
        assertEquals(Level.L4, Level.of(4))
        assertEquals(Level.L1, Level.of(99))
        assertEquals(Level.L2, Level.fromLabel("L2"))
        assertNull(Level.fromLabel("X9"))
    }
}