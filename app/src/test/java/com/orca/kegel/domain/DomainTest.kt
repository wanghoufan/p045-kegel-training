package com.orca.kegel.domain

import com.orca.kegel.data.AppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 领域层单测（纯 Kotlin，无需真机）：
 *  - V2.2 六级剂量矩阵数值一致性（每场固定次数 × 会话数）
 *  - 组数拆分与单次/每日时长公式
 *  - 频次（会话次数）降量公式
 *  - 双维额度的取小扣减、中止不返还、日上限不可越界
 *  - 整日半量折半
 *  - 脚本展开（H 含收紧起始、耐力每组 ≤10、快速每回合 ≤20、组间 30 秒）与推进器
 *  - 摸底建议阈值（沿用）
 */
class DomainTest {

    // ---------- 六级剂量矩阵一致性 ----------

    @Test
    fun levelSpecs_matchV2Matrix() {
        val expected = mapOf(
            Level.L1 to intArrayOf(3, 6, 15, 1, 4, 15, 3),
            Level.L2 to intArrayOf(4, 6, 18, 1, 2, 24, 3),
            Level.L3 to intArrayOf(5, 5, 28, 1, 2, 40, 4),
            Level.L4 to intArrayOf(6, 6, 32, 1, 2, 48, 4),
            Level.L5 to intArrayOf(8, 8, 45, 1, 2, 80, 5),
            Level.L6 to intArrayOf(10, 10, 60, 1, 2, 120, 6),
        )
        assertEquals(6, Level.entries.size)
        expected.forEach { (lv, v) ->
            val s = LevelRules.spec(lv)
            assertEquals("hold $lv", v[0], s.holdSec)
            assertEquals("rest $lv", v[1], s.restSec)
            assertEquals("dailyE $lv", v[2], s.dailyEndurance)
            assertEquals("fastSec $lv", v[3], s.fastSec)
            assertEquals("fastRest $lv", v[4], s.fastRestSec)
            assertEquals("dailyF $lv", v[5], s.dailyFast)
            assertEquals("sessions $lv", v[6], s.fullSessions)
            // 每次会话次数 = 日量 ÷ 会话数，必须整除且与表一致
            assertEquals("e/session $lv", v[2] / v[6], s.endurancePerSession)
            assertEquals("f/session $lv", v[5] / v[6], s.fastPerSession)
            assertEquals("整除 $lv", 0, v[2] % v[6])
            assertEquals("整除 $lv", 0, v[5] % v[6])
        }
    }

    @Test
    fun dailyTotals_matchAndNeverExceedCap() {
        assertEquals(30, LevelRules.spec(Level.L1).dailyTotal)
        assertEquals(42, LevelRules.spec(Level.L2).dailyTotal)
        assertEquals(68, LevelRules.spec(Level.L3).dailyTotal)
        assertEquals(80, LevelRules.spec(Level.L4).dailyTotal)
        assertEquals(125, LevelRules.spec(Level.L5).dailyTotal)
        assertEquals(180, LevelRules.spec(Level.L6).dailyTotal)
        assertTrue(LevelRules.specs.values.all { it.dailyTotal <= LevelRules.DAILY_LIMIT })
        assertEquals(240, LevelRules.DAILY_LIMIT)
        // 保持含收紧起始 ≤10 秒
        assertTrue(LevelRules.specs.values.all { it.holdSec <= 10 })
        // 每场耐力 ≤10（1 组）、快速 ≤20（1 回合）、合计 ≤40
        assertTrue(LevelRules.specs.values.all { it.endurancePerSession <= LevelRules.GROUP_MAX })
        assertTrue(LevelRules.specs.values.all { it.fastPerSession <= LevelRules.FAST_GROUP_MAX })
        assertTrue(LevelRules.specs.values.all { it.endurancePerSession + it.fastPerSession <= LevelRules.SESSION_MAX })
    }

    // ---------- 组数与时长 ----------

    @Test
    fun groupsSplitAtTen() {
        assertEquals(0, LevelRules.groups(0))
        assertEquals(0, LevelRules.groups(-3))
        assertEquals(1, LevelRules.groups(1))
        assertEquals(1, LevelRules.groups(10))
        assertEquals(2, LevelRules.groups(11))
        assertEquals(2, LevelRules.groups(15))
        assertEquals(2, LevelRules.groups(20))
        assertEquals(3, LevelRules.groups(21))
        assertEquals(6, LevelRules.groups(60))
    }

    @Test
    fun fastGroupsSplitAtTwenty() {
        assertEquals(0, LevelRules.fastGroups(0))
        assertEquals(1, LevelRules.fastGroups(1))
        assertEquals(1, LevelRules.fastGroups(20))
        assertEquals(2, LevelRules.fastGroups(21))
        assertEquals(1, LevelRules.fastGroups(12)) // L4 每回合 12 ≤ 20 → 1 回合
    }

    @Test
    fun standardSessionSeconds_matchV2Table() {
        // 表：L1..L6 单次 = 106 / 120 / 136 / 168 / 228 / 296 秒
        assertEquals(106, LevelRules.standardSessionSeconds(Level.L1))
        assertEquals(120, LevelRules.standardSessionSeconds(Level.L2))
        assertEquals(136, LevelRules.standardSessionSeconds(Level.L3))
        assertEquals(168, LevelRules.standardSessionSeconds(Level.L4))
        assertEquals(228, LevelRules.standardSessionSeconds(Level.L5))
        assertEquals(296, LevelRules.standardSessionSeconds(Level.L6))
    }

    @Test
    fun standardDailySeconds_matchV2Table() {
        // 单次 × 会话数（N=3/3/4/4/5/6）
        assertEquals(318, LevelRules.standardDailySeconds(Level.L1))
        assertEquals(360, LevelRules.standardDailySeconds(Level.L2))
        assertEquals(544, LevelRules.standardDailySeconds(Level.L3))
        assertEquals(672, LevelRules.standardDailySeconds(Level.L4))
        assertEquals(1140, LevelRules.standardDailySeconds(Level.L5))
        assertEquals(1776, LevelRules.standardDailySeconds(Level.L6))
    }

    // ---------- 频次（会话次数）降量公式 ----------

    @Test
    fun dailyAllocation_isMatrixFullDose() {
        Level.entries.forEach { lv ->
            val spec = LevelRules.spec(lv)
            val a = Quota.daily(lv, spec.fullSessions, halfDose = false)
            assertEquals(spec.dailyEndurance, a.eQuota)
            assertEquals(spec.dailyFast, a.fQuota)
            assertEquals(spec.dailyTotal, a.total)
        }
    }

    @Test
    fun l6_sessionSplit_bySessionCount() {
        // L6 每次固定 10+20；日量 = 每次 × N
        val a6 = Quota.daily(Level.L6, 6, halfDose = false)
        assertEquals(10 to 20, Quota.forSession(a6, 1))
        assertEquals(10 to 20, Quota.forSession(a6, 6))
        assertEquals(180, a6.total)

        // L6 选 3 → 每次仍 10+20，日 90
        val a3 = Quota.daily(Level.L6, 3, halfDose = false)
        assertEquals(90, a3.total)
        assertEquals(10 to 20, Quota.forSession(a3, 1))
        assertEquals(10 to 20, Quota.forSession(a3, 3))

        // L6 选 1 → 每次 10+20，日 30
        val a1 = Quota.daily(Level.L6, 1, halfDose = false)
        assertEquals(30, a1.total)
        assertEquals(10 to 20, Quota.forSession(a1, 1))

        // L6 选 4 → 40+80 = 120
        assertEquals(120, Quota.daily(Level.L6, 4, halfDose = false).total)
    }

    @Test
    fun l2_threeSessions_uniformPerSession() {
        // V2.2：每次会话次数全天一致（不得出现 17/17/16、12/11 式余数分摊）
        val a = Quota.daily(Level.L2, 3, halfDose = false)
        assertEquals(42, a.total)
        (1..3).forEach { i -> assertEquals(6 to 8, Quota.forSession(a, i)) }
    }

    @Test
    fun forSession_uniformAtDefaultForAllLevels() {
        Level.entries.forEach { lv ->
            val spec = LevelRules.spec(lv)
            val a = Quota.daily(lv, spec.fullSessions, halfDose = false)
            (1..spec.fullSessions).forEach { i ->
                assertEquals("$lv@$i", spec.endurancePerSession to spec.fastPerSession, Quota.forSession(a, i))
            }
        }
    }

    @Test
    fun forSession_outOfRangeIsEmpty() {
        val a = Quota.daily(Level.L6, 6, halfDose = false)
        assertEquals(0 to 0, Quota.forSession(a, 0))
        assertEquals(0 to 0, Quota.forSession(a, 7))
    }

    // ---------- 双维额度取小 / 中止不返还 / 上限 ----------

    @Test
    fun build_usesTypeRemainingAndNeverMovesFastToEndurance() {
        // L3 每次 7+10；3 场 → 日耐力 21、日快速 30
        val r = Quota.build(Level.L3, sessions = 3, halfDose = false, sessionIndex = 1, usedE = 16, usedF = 0)
        val p = (r as PlanResult.Ok).plan
        // 耐力余量只剩 21-16=5 → 不得用快速额补耐力
        assertEquals(5, p.enduranceReps)
        assertEquals(10, p.fastReps)
        assertTrue(p.reducedByRemaining)
    }

    @Test
    fun build_degradesToSingleTypeWhenOneQuotaExhausted() {
        val r = Quota.build(Level.L3, sessions = 3, halfDose = false, sessionIndex = 1, usedE = 21, usedF = 0)
        val p = (r as PlanResult.Ok).plan
        assertEquals(0, p.enduranceReps)
        assertEquals(10, p.fastReps)
    }

    @Test
    fun build_blockedWhenBothQuotasExhausted() {
        val r = Quota.build(Level.L3, sessions = 3, halfDose = false, sessionIndex = 1, usedE = 21, usedF = 30)
        assertTrue(r is PlanResult.Blocked)
        assertEquals(0, (r as PlanResult.Blocked).remainingE)
        assertEquals(0, r.remainingF)
    }

    @Test
    fun build_blockedWhenSessionsExhausted() {
        val r = Quota.build(Level.L6, sessions = 6, halfDose = false, sessionIndex = 7, usedE = 0, usedF = 0)
        assertTrue(r is PlanResult.Blocked)
    }

    @Test
    fun build_neverExceedsSessionAndDailyCaps() {
        Level.entries.forEach { lv ->
            val n = LevelRules.spec(lv).fullSessions
            for (i in 1..n) {
                val alloc = Quota.daily(lv, n, halfDose = false)
                val r = Quota.build(lv, n, false, i, 0, 0)
                if (r is PlanResult.Ok) {
                    val p = r.plan
                    assertTrue("${lv}@${i} session cap", p.totalContractions <= LevelRules.SESSION_MAX)
                    assertTrue("${lv}@${i} groups", p.enduranceReps <= 40 && p.fastReps <= 40)
                }
                assertTrue("${lv} daily cap", alloc.total <= LevelRules.DAILY_LIMIT)
            }
        }
    }

    // ---------- 整日半量 ----------

    @Test
    fun halfDose_halvesBothTypes() {
        val l2 = Quota.daily(Level.L2, 3, halfDose = true)
        assertEquals(9, l2.eQuota)
        assertEquals(12, l2.fQuota)
        assertEquals(21, l2.total) // L2 半量 = 9+12

        val l6 = Quota.daily(Level.L6, 6, halfDose = true)
        assertEquals(30, l6.eQuota)
        assertEquals(60, l6.fQuota)
        assertEquals(90, l6.total) // L6 半量 = 30+60
    }

    @Test
    fun halfDose_zeroQuotaBranchDoesNotDivideByZero() {
        val a = Quota.daily(Level.L1, 3, halfDose = true)
        assertEquals(7, a.eQuota)
        assertEquals(7, a.fQuota)
    }

    // ---------- AppState 额度派生（中止不返还 / 已开始即扣） ----------

    private fun record(
        day: Long,
        enduranceReps: Int,
        fastReps: Int,
        enduranceStarted: Int = enduranceReps,
        fastStarted: Int = fastReps,
        completed: Boolean = true,
    ) = SessionRecord(
        epochDay = day,
        timestampMs = day,
        level = Level.L6,
        enduranceReps = enduranceReps,
        enduranceHoldSeconds = enduranceReps * 10,
        fastReps = fastReps,
        durationSeconds = 60,
        completed = completed,
        enduranceStarted = enduranceStarted,
        fastStarted = fastStarted,
    )

    @Test
    fun appState_usageCountsStartedContractions() {
        val day = 20_000L
        val st = AppState(currentLevel = Level.L6, history = listOf(record(day, enduranceReps = 3, fastReps = 2, enduranceStarted = 5, fastStarted = 4)))
        assertEquals(5, st.usedE(day))
        assertEquals(4, st.usedF(day))
        assertEquals(9, st.contractionsOn(day))
    }

    @Test
    fun appState_abortDoesNotReturnQuota() {
        val day = 20_001L
        val st = AppState(
            currentLevel = Level.L6,
            history = listOf(record(day, enduranceReps = 0, fastReps = 0, enduranceStarted = 6, fastStarted = 3, completed = false)),
        )
        assertEquals(6, st.usedE(day))
        assertEquals(3, st.usedF(day))
        assertEquals(9, st.contractionsOn(day))
    }

    @Test
    fun appState_remainingIsPerTypeNonNegative() {
        val day = 20_002L
        val st = AppState(
            currentLevel = Level.L3,
            history = listOf(record(day, enduranceReps = 30, fastReps = 40)),
        )
        assertEquals(0, st.remainingE(day))
        assertEquals(0, st.remainingF(day))
        val blocked = Quota.build(Level.L3, 3, false, st.nextSessionIndex(day), st.usedE(day), st.usedF(day))
        assertTrue(blocked is PlanResult.Blocked)
    }

    @Test
    fun appState_halfDoseFrozenForDay() {
        val day = 20_003L
        val st = AppState(currentLevel = Level.L6, halfDoseDate = day, halfDoseValue = true)
        assertTrue(st.halfDoseOn(day))
        assertFalse(st.halfDoseOn(day + 1))
        assertEquals(30, st.allocation(day).eQuota)
        // 次日半量失效 → 恢复满量 60+120
        assertEquals(180, st.allocation(day + 1).eQuota + st.allocation(day + 1).fQuota)
    }

    @Test
    fun appState_sessionIndexAdvancesWithRecords() {
        val day = 20_004L
        val st = AppState(history = listOf(record(day, 10, 10), record(day, 10, 10)))
        assertEquals(3, st.nextSessionIndex(day))
    }

    @Test
    fun appState_dayFreezeNeverRaisesButLowersImmediately() {
        val day = 20_005L
        // 首次规划冻结在 L3 / 3 会话
        val frozen = AppState(
            currentLevel = Level.L3,
            sessionCount = 3,
            freezeDate = day,
            freezeLevelNum = 3,
            freezeSessions = 3,
        )
        assertEquals(Level.L3, frozen.dayLevel(day))
        assertEquals(3, frozen.daySessions(day))
        assertEquals(51, frozen.allocation(day).total)

        // 当天调高到 L6 / 6 会话 → 当日仍按 L3（不抬额）
        val raised = frozen.copy(currentLevel = Level.L6, sessionCount = 6)
        assertEquals(Level.L3, raised.dayLevel(day))
        assertEquals(3, raised.daySessions(day))
        assertEquals(51, raised.allocation(day).total)

        // 当天调低到 L1 → 立即收紧
        val lowered = frozen.copy(currentLevel = Level.L1)
        assertEquals(Level.L1, lowered.dayLevel(day))
        assertEquals(30, lowered.allocation(day).total)

        // 次日恢复为 live 选择
        assertEquals(Level.L6, raised.dayLevel(day + 1))
        assertEquals(6, raised.daySessions(day + 1))
    }

    // ---------- P1-1 摸底扣额（按实际已开始、只归当日、不计会话） ----------

    @Test
    fun appState_assessmentUsageCountsOnlyForItsDay() {
        val day = 21_000L
        val st = AppState(currentLevel = Level.L3, assessmentDate = day, assessmentUsedE = 4)
        // 摸底实际已开始的 4 次耐力收缩进入当日已用
        assertEquals(4, st.usedE(day))
        // 次日不继承
        assertEquals(0, st.usedE(day + 1))
        // 不产生完成会话/统计，也不消费时段（会话序号仍为 1）
        assertEquals(1, st.nextSessionIndex(day))
        assertEquals(0, HistoryStats.of(st.history).sessions)
        // 剩余额度 = 当日耐力额（L3 满量 28）− 摸底已用
        assertEquals(24, st.remainingE(day))
    }

    // ---------- P1-3 疲劳即停与疲劳后半量 ----------

    @Test
    fun halfDose_floorsHalfWithFloorGuardAtLeastOne() {
        // 公式：E_H=floor(E/2)、F_H=floor(F/2)（非零时至少 1）
        val l2 = Quota.daily(Level.L2, 3, halfDose = true)
        assertEquals(9, l2.eQuota)
        assertEquals(12, l2.fQuota)
        val l6 = Quota.daily(Level.L6, 6, halfDose = true)
        assertEquals(30, l6.eQuota)
        assertEquals(60, l6.fQuota)
    }

    @Test
    fun appState_pendingHalfDoseAppliesOnlyNextDay() {
        val day = 21_200L
        val st = AppState(currentLevel = Level.L6, pendingHalfDate = day + 1, pendingHalfValue = true)
        assertFalse(st.halfDoseOn(day))            // 当日不受影响
        assertTrue(st.halfDoseOn(day + 1))         // 次日半量生效
        assertFalse(st.halfDoseOn(day + 2))        // 仅次日一天
        assertEquals(180, st.allocation(day).total)
        assertEquals(90, st.allocation(day + 1).total) // L6 半量 = 30+60
    }

    // ---------- P2-5 跨午夜按发生日扣额 ----------

    @Test
    fun appState_startedContractionsAttributeToOccurrenceDay() {
        val d1 = 21_300L
        val d2 = d1 + 1
        val rec = SessionRecord(
            epochDay = d2,
            timestampMs = 1_700_000_000_000L,
            level = Level.L6,
            enduranceReps = 0,
            enduranceHoldSeconds = 0,
            fastReps = 0,
            durationSeconds = 700,
            completed = true,
            enduranceStarted = 12,
            fastStarted = 10,
            startedByDay = listOf(DayUsage(d1, 6, 5), DayUsage(d2, 6, 5)),
        )
        val st = AppState(history = listOf(rec))
        // 跨午夜：按收缩开始时刻的本地日归属
        assertEquals(6, st.usedE(d1))
        assertEquals(6, st.usedE(d2))
        assertEquals(5, st.usedF(d1))
        assertEquals(5, st.usedF(d2))
    }

    // ---------- P2-6 doseVersion 可追溯 ----------

    @Test
    fun sessionRecord_legacyDoseVersionDefaultsToV1() {
        // 旧记录缺字段 → 回退 1（可追溯，不受 V2 上限追溯改写）；V2 记录显式 2
        assertEquals(1, SessionRecord(100, 1, Level.L3, 5, 25, 5, 120, true).doseVersion)
        assertEquals(2, SessionPlan.DOSE_VERSION)
    }

    // ---------- 脚本展开 ----------
    private fun plan(level: Level, sessions: Int, index: Int = 1): SessionPlan =
        (Quota.build(level, sessions, halfDose = false, sessionIndex = index, usedE = 0, usedF = 0) as PlanResult.Ok).plan

    @Test
    fun scriptBuilder_l1StepsAndTotal() {
        val plan = plan(Level.L1, 3) // e=5,f=5,H=3,R=6,r=4
        val steps = ScriptBuilder.build(plan)
        // READY + 5*(HOLD,RELAX) + REST + 5*(QT,QR) + DONE
        assertEquals(1 + 5 * 2 + 1 + 5 * 2 + 1, steps.size)
        assertEquals(106_000L, steps.sumOf { it.durationMs })
        assertEquals(Phase.READY, steps.first().phase)
        assertEquals(Phase.DONE, steps.last().phase)
        // 保持步＝H 秒（含收紧起始，无额外 2 秒）
        val hold = steps.first { it.phase == Phase.HOLD }
        assertEquals(3000L, hold.durationMs)
        assertEquals(CueId.TIGHTEN, hold.lead)
        // 组间休息 30 秒
        val rest = steps.first { it.section == Section.REST }
        assertEquals(30_000L, rest.durationMs)
        assertEquals(CueId.REST, rest.lead)
    }

    @Test
    fun scriptBuilder_l4OneEnduranceGroupOneFastRound() {
        // V2.2 L4 选 3：每次 8 耐力（1 组）+ 12 快速（1 回合）→ 仅 1 段 30 秒休息
        val plan = plan(Level.L4, 3)
        assertEquals(8, plan.enduranceReps)
        assertEquals(12, plan.fastReps)
        val steps = ScriptBuilder.build(plan)
        assertEquals(1, plan.enduranceGroups)
        assertEquals(1, plan.fastGroups)
        val rests = steps.filter { it.section == Section.REST }
        assertEquals(1, rests.size)
        assertTrue(rests.all { it.durationMs == 30_000L })
        assertEquals(168_000L, steps.sumOf { it.durationMs })
        // 耐力每组 ≤10 次
        val enduranceReps = steps.count { it.phase == Phase.HOLD }
        assertEquals(8, enduranceReps)
    }

    @Test
    fun scriptBuilder_holdHasCountdownTails() {
        val steps = ScriptBuilder.build(plan(Level.L1, 3))
        val hold = steps.first { it.phase == Phase.HOLD }
        assertTrue(hold.countdown)
        assertEquals(listOf(CueId.NUM3, CueId.NUM2, CueId.NUM1), hold.tails.map { it.cue })
        assertEquals(listOf(0L, 1000L, 2000L), hold.tails.map { it.atMs })
    }

    @Test
    fun scriptBuilder_longHoldAnnouncesHoldCue() {
        val hold = ScriptBuilder.build(plan(Level.L6, 6)).first { it.phase == Phase.HOLD }
        assertTrue(hold.tails.any { it.cue == CueId.HOLD })
        // 更长的保持里「保持」提示位于 3-2-1 倒数之前
        assertTrue(hold.tails.first { it.cue == CueId.HOLD }.atMs < hold.tails.first { it.cue == CueId.NUM3 }.atMs)
    }

    @Test
    fun scriptBuilder_transitionRestAnnouncesFastIntro() {
        val steps = ScriptBuilder.build(plan(Level.L3, 3))
        val rest = steps.first { it.section == Section.REST }
        assertTrue(rest.tails.any { it.cue == CueId.FAST_INTRO })
    }

    // ---------- 推进器 ----------

    @Test
    fun sessionScript_runsToFinishAndEmitsAllCues() {
        val script = SessionScript(ScriptBuilder.build(plan(Level.L1, 3)))
        assertEquals(CueId.READY, script.start())
        val cues = mutableSetOf<CueId>()
        var guard = 0
        while (!script.isFinished && guard++ < 200000) {
            cues += script.tick(250).cues
        }
        assertTrue(script.isFinished)
        assertTrue(cues.containsAll(
            listOf(
                CueId.TIGHTEN, CueId.RELAX, CueId.REST, CueId.FAST_INTRO,
                CueId.FAST_TIGHTEN, CueId.FAST_RELAX, CueId.DONE,
                CueId.NUM3, CueId.NUM2, CueId.NUM1, CueId.QUALITY,
            )
        ))
        assertEquals(script.totalMs, script.elapsedTotalMs())
    }

    @Test
    fun sessionScript_countsEnduranceAndFastReps() {
        val script = SessionScript(ScriptBuilder.build(plan(Level.L1, 3)))
        script.start()
        var endurance = 0
        var fast = 0
        var guard = 0
        while (!script.isFinished && guard++ < 200000) {
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
    fun sessionScript_extendsOnlyRestStep() {
        val rest = Step(Phase.REST, 30_000L, Section.REST, lead = CueId.REST)
        val script = SessionScript(listOf(rest))
        script.start()
        assertEquals(30_000L, script.totalMs)
        assertTrue(script.extendCurrent(5_000L))
        assertEquals(35_000L, script.totalMs)
        assertEquals(35_000L, script.current()!!.durationMs)

        val hold = SessionScript(listOf(Step(Phase.HOLD, 3_000L, Section.ENDURANCE)))
        hold.start()
        assertFalse(hold.extendCurrent(5_000L))
        assertEquals(3_000L, hold.totalMs)
    }

    // ---------- 摸底建议阈值（沿用） ----------

    @Test
    fun recommender_vetoesCompensatingAndPoorRelax() {
        val base = AssessmentResult(0, ControlQuality.CLEAR, 10, 10, 10, RelaxQuality.GOOD)
        assertNull(Recommender.recommend(base.copy(controlQuality = ControlQuality.COMPENSATING)))
        assertNull(Recommender.recommend(base.copy(relaxQuality = RelaxQuality.POOR)))
    }

    @Test
    fun recommender_shortBoardPicksLowestQualifyingLevel() {
        assertEquals(Level.L6, Recommender.recommend(AssessmentResult(0, ControlQuality.CLEAR, 10, 10, 10, RelaxQuality.GOOD)))
        assertEquals(Level.L1, Recommender.recommend(AssessmentResult(0, ControlQuality.CLEAR, 3, 5, 5, RelaxQuality.GOOD)))
        assertEquals(Level.L3, Recommender.recommend(AssessmentResult(0, ControlQuality.CLEAR, 5, 8, 8, RelaxQuality.GOOD)))
        // 2/10/10 无 L1 建议 → 回退最低 L1
        assertEquals(Level.L1, Recommender.recommend(AssessmentResult(0, ControlQuality.CLEAR, 2, 10, 10, RelaxQuality.GOOD)))
        // 10/10/10 建议 L6（但不授予 240，量权限在 Quota/上限层面另定）
        assertEquals(Level.L6, Recommender.recommend(AssessmentResult(0, ControlQuality.CLEAR, 10, 10, 10, RelaxQuality.GOOD)))
    }

    @Test
    fun recommend_doesNotUpgradeFromSingleStrongDimension() {
        assertEquals(Level.L1, Recommender.recommend(AssessmentResult(0, ControlQuality.CLEAR, 10, 1, 1, RelaxQuality.GOOD)))
    }

    @Test
    fun suggestUpgrade_requiresAllDimensions() {
        val meetingL2 = AssessmentResult(0, ControlQuality.CLEAR, 4, 6, 6, RelaxQuality.GOOD)
        assertEquals(Level.L2, Recommender.suggestUpgrade(Level.L1, meetingL2))
        assertNull(Recommender.suggestUpgrade(Level.L1, meetingL2.copy(fastReps = 5)))
        assertNull(Recommender.suggestUpgrade(Level.L6, meetingL2))
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
        assertEquals(5, s.longestHoldSeconds)
        assertEquals(20, s.fastRepsTotal)
    }

    @Test
    fun historyInsights_summaryAndDaily() {
        val d = 21_500L
        val recs = listOf(
            SessionRecord(d, 1, Level.L3, 7, 70, 10, 136, true),
            SessionRecord(d, 2, Level.L3, 0, 0, 0, 60, false, SafetyReason.PAIN),
            SessionRecord(d - 1, 3, Level.L1, 5, 30, 5, 106, true),
            SessionRecord(d - 3, 4, Level.L1, 5, 30, 5, 106, true),
        )
        val sum = HistoryInsights.summary(recs)
        assertEquals(4, sum.totalSessions)
        assertEquals(3, sum.totalDays)
        assertEquals(408, sum.totalDurationSeconds) // 136+60+106+106
        assertEquals(17, sum.totalEnduranceReps)
        assertEquals(20, sum.totalFastReps)
        assertEquals(37, sum.totalReps)
        assertEquals(130, sum.totalHoldSeconds)

        val daily = HistoryInsights.daily(recs, days = 5, todayEpochDay = d)
        assertEquals(5, daily.size)
        assertEquals(d - 4, daily.first().epochDay)
        assertEquals(d, daily.last().epochDay)
        assertEquals(2, daily.last().sessions)
        assertEquals(196, daily.last().durationSeconds) // 136+60
        assertEquals(17, daily.last().totalReps)
        assertEquals(0, daily[2].sessions) // d-2 无训练
        assertTrue(HistoryInsights.daily(recs, days = 0, todayEpochDay = d).isEmpty())
    }

    @Test
    fun levelOf_and_fromLabel() {
        assertEquals(Level.L4, Level.of(4))
        assertEquals(Level.L1, Level.of(99))
        assertEquals(Level.L2, Level.fromLabel("L2"))
        assertNull(Level.fromLabel("X9"))
    }
}
