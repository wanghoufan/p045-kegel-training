# CODE REVIEW｜DEV-V2 剂量体系（TASK-020 返工复检 R2）

- Task: TASK-020 返工（修复 CODE_REVIEW-V2.0-DEV2.md 三条 P1 ＋可行 P2）
- Commit: 未提交（`git diff` 工作区，2026-10-13 复检时点）
- Reviewer: code-reviewer（codebuddy/glm-5.3-flash，独立 Session，只读复检）
- 需求真源: `docs/pm/PRODUCT_PLAN_V2.0.md`（FR-002 安全闸、FR-005 第 5/6 条、AC-31/56/57/58/61）＋用户修正令 B2
- Result: **PASS（可进 QA）** — P0=0，P1=0（三条 P1 均闭合），P2=3（不阻塞）
- 测试实证：本轮复检实际重跑 `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL，XML 汇总 `tests=43 failures=0`（与 builder 声称一致）。

> 审查范围：只读 diff 与相关上下文；未改任何业务代码；禁项（风格/命名/依赖）未涉及。

## 一、上轮 P1 逐条复核（不接受声称，逐条对代码与测试）

### P1-1 摸底扣额 — 已闭合（残留小旁路降级为 P2-1）
- 计数口径：保持测试每次「开始计时/重测」启动即计（`AssessmentScreen.kt:297` `onStart()` → `holdAttempts++`，`AssessmentScreen.kt:144`）；逐轮耐力每轮进入收紧即计（`AssessmentScreen.kt:326` `onStart()` → `repStarts++`，`AssessmentScreen.kt:150`）。均为「实际已开始」口径，重测多次按次累加，正确。
- 落账：结果确认时 `addAssessmentUsage(epochDay, holdAttempts + repStarts)`（`AssessmentScreen.kt:95`）→ `AppRepository.addAssessmentUsage`（`AppRepository.kt:307-312`，同日累加、跨日重置）→ `AppState.usedE` 合入当日已用（`AppRepository.kt:79`）。不计完成会话（`nextSessionIndex` 只数 history，`DomainTest.kt:337` 断言仍为 1）、不进统计（`DomainTest.kt:338`）。
- 快速滑块仅自报不扣（`AssessmentScreen.kt:162-167` 无任何计数路径）；动作教育（step 0）不扣。正确。
- 余量不足提示且不锁摸底：`assess_quota_note` 在 step≤2 且 `remainingE<1` 时显示（`AssessmentScreen.kt:122-128`），无阻断。中英齐备（values:140 / values-en:140）。
- 归属日正确：仅当日生效，次日不继承（`DomainTest.kt:329-341` 独立硬编码断言 4/0/26）。
- **残留旁路（降级 P2-1）**：扣额仅发生在最后一步确认时；摸底中途「跳过/返回」退出（`AssessmentScreen.kt:106`、`onClose`）时已实际开始的收缩不落账。量有界（≤≈20 次/日）且需用户主动中断，见 P2-1。

### P1-2 会话间隔 ≥2 小时 — 已闭合
- 闸门：`SessionSpacing.MIN_GAP_MS = 2h`、`canStartNext`、`nextAllowedAt`（`Plan.kt:91-101`）；`todayPlan` 首判间隔，未满即返回 `IntervalBlocked`（`AppViewModel.kt:97-100`），**先于**额度计算，`startToday` 对 `IntervalBlocked` 不发 Start（`AppViewModel.kt:121`）；服务端另有 RUNNING/PAUSED 防重入（`TrainingService.kt:213-214`）。
- 首页可见说明＋延后/休息出口：`IntervalBlocked` 分支显示 `home_interval_blocked`＋剩余分钟 `home_interval_wait`（`KegelApp.kt:223-234`），该态下**无任何启动入口**；中英文案齐备（values:27-28 / values-en:27-28）。
- 低频不误拦：间隔只看最近一条记录的 `timestampMs`（`AppRepository.kt:99`），与 `nextSessionIndex`（按当日条数）解耦；3 次/日只要间隔 ≥2h 均放行。边界测试：恰好 2 小时放行、差 1ms 阻断、首次无记录放行（`DomainTest.kt:346-363`，独立硬编码）。
- 跨日紧贴（如 23:00 结束、次日 00:30 启动）按绝对间隔阻断——符合安全闸「恢复不能保证」语义，非误拦。

### P1-3 疲劳即停＋疲劳次日半量 — 已闭合
- 枚举与出口：`SafetyReason.FATIGUE`（`Plan.kt:44`）；训练页安全出口首项「疲劳、动作质量下降或无法正常呼吸」（`TrainingScreens.kt:265`），确认后 `SafetyExit(FATIGUE)` → `finishSession(safety=FATIGUE)` 落库 `safetyReason`（`TrainingService.kt:199,330-388`）。
- 次日半量：`safety == FATIGUE` → `scheduleHalfDose(明日)`（`TrainingService.kt:385-388`）→ 独立键 `pending_half_date/value`（`AppRepository.kt:298-301`），`halfDoseOn` 仅在 pending 当日生效（`AppRepository.kt:126-128`），不影响当日冻结额。结束页向用户说明（`TrainingScreens.kt:344-352`＋`finish_fatigue_note` 双语，values:96 / values-en:96）。
- 半量折半：`halfFloor = max(1, v/2)`（`Plan.kt:139`）＝floor 一半且非零 ≥1；L2=12+10=22、L6=60+60=120 有断言（`DomainTest.kt:202-212,368-376`）。
- 受剩余额度约束：半量只降分配（`Quota.daily`），`usedE/usedF` 不动，`build` 再按双维余量取小——半量不会抬高或返还已用量。
- 次日半量仅一天（`DomainTest.kt:379-387` 断言 day+2 恢复满量）。安全出口四项枚举双语文案齐备。
- **新发现（P2-2）**：次日半量可被首页半量开关一键关闭，见下。

### 上轮 P2 处理情况
- **跨午夜按发生日扣额（原 P2-2）— 已闭合**：`startedByDay` 分桶（`History.kt:9-14,45`），服务端进入 HOLD/QUICK_TIGHTEN 时按当时本地日 `merge` 计桶（`TrainingService.kt:251-259`），落库按桶（`TrainingService.kt:362-379`）；读取侧桶优先、旧记录回退 `epochDay`（`AppRepository.kt:71-90`）。专项测试 `appState_startedContractionsAttributeToOccurrenceDay`（`DomainTest.kt:392-414`）断言 6/6、5/5 分日归属。
- **死代码 `startLevelPreview`（原 P2-1）— 已闭合**：全仓 grep 无匹配，已删除。
- **doseVersion 可追溯（原 P2-3）— 已闭合**：`SessionRecord.doseVersion` 默认 **1**（`History.kt:43`，V1 旧记录缺字段解码回退 1），V2 记录显式写 2（`TrainingService.kt:378`）；旧 40 上限历史不被追溯改写（读取只按 started/reps 保守计额，`AppRepository.kt:76,88`）。专项测试 `sessionRecord_legacyDoseVersionDefaultsToV1`（`DomainTest.kt:419-423`）。
- **2/10/10 兜底建议 L1（原 P2-4）— 未处理**：`Assessment.kt:51` 仍 `return Level.L1`，与 V2.0 AC-07「2/10/10 无 L1 建议、显示教育/求助」仍不符；单测 `recommender_shortBoardPicksLowestQualifyingLevel:558` 还固化该行为。维持 **P2 未闭合**（本轮计 P2-3）。

## 二、防回归（硬约束未被回退）

- 常量未动：`DAILY_LIMIT=240`、`SESSION_MAX=40`、`GROUP_MAX=10`、`GROUP_REST_SEC=30`（`Levels.kt:76-85`）；六级 `holdSec ≤10` 含收紧起始（`Levels.kt` spec ＋ `DomainTest.kt:58`）。
- 单会话 ≤40：`Q=min(E_H+F_H, 40×N)`（`Plan.kt:132`）＋第 7 时段越界 Blocked（`DomainTest.kt:178-180`）。
- 组间/跨类 30 秒可延长不可缩短：`extendCurrent` 仅 REST、仅加（`Plan.kt:361-367`）；服务端仅 RUNNING 受理 `ExtendRest`（`TrainingService.kt:204-208`）；UI 仅 REST 且未暂停显示（`TrainingScreens.kt:165-170`）；专项测试在（`DomainTest.kt:528-541`）。
- 跨类型不互转：`e=min(e_i,rE)、f=min(f_i,rF)`（`Plan.kt:184-185`）＋专项测试（`DomainTest.kt:151-158`）。
- 中止不返还：`enduranceStarted/fastStarted` 进入即计、随记录落库（`TrainingService.kt:244-265,376-377`）＋测试（`DomainTest.kt:253-262`）。
- 暂停/恢复幂等：`countedUpTo` 游标保证恢复后不重计（`TrainingService.kt:245-264`）；暂停期间引擎协程取消不推进（`TrainingService.kt:311-318`）。
- 既有 PAIN/URINARY/RELAX_FAIL 出口、中英双语、主题三态：`SafetyExitDialog` 四项并存（`TrainingScreens.kt:265-268`）；strings 无缺键；KegelApp/TrainingScreen 未触碰主题逻辑。
- 当日冻结只收不减、调整级即时收紧：逻辑未改（`AppRepository.kt:112-133`）＋测试（`DomainTest.kt:296-324`）。

## 三、测试有效性（43 例，实测全绿）

- 新增 7 例全部**独立硬编码期望值**，无「断言等于实现」假绿：摸底归属日（21_000/4/26）、间隔边界（30 分钟阻断、2h 整放行、−1ms 阻断、首次放行）、疲劳次日半量（当日 240 / 次日 120 / 后日 240）、半量折半（12+10、60+60）、跨午夜分桶（6/6、5/5）、doseVersion（1 vs 2）。边界覆盖：2 小时整 ✓、240 触顶（`l6_sessionSplit` 240/160/120/40＋`build_neverExceedsSessionAndDailyCaps`）✓、零余量（`build_blockedWhenBothQuotasExhausted`、`appState_remainingIsPerTypeNonNegative`）✓；**半量后剩 1 次的极端分支（`halfFloor(1)=1`）无专项用例**——小缺口，不影响结论。

## 四、Findings

### P0（0 条）
无。

### P1（0 条）
上轮三条 P1 均已闭合（证据见第一节），无新增 P1。

### P2（3 条，不阻塞进 QA，建议随后修）

- **P2-1｜摸底中途退出不扣已开始收缩（P1-1 残留旁路）**
  位置：`app/src/main/java/com/orca/kegel/ui/AssessmentScreen.kt:95`（仅最后一步 `next()` 时 `addAssessmentUsage`）；跳过/返回路径（`AssessmentScreen.kt:104-107`）不落账。
  影响：用户做了保持测试/若干轮耐力后中途退出，这些实际已开始的收缩不占当日额度，240 上限仍可被突破约 ≤20 次/日（有界、需用户主动中断）。
  建议：改为在 `holdAttempts++`/`repStarts++` 时即时累加落账（或退出/跳过时 flush），与「按实际已开始扣」口径完全一致。

- **P2-2｜疲劳次日半量可被首页开关一键解除（安全门恢复条件未被强制）**
  位置：`app/src/main/java/com/orca/kegel/data/AppRepository.kt:282-291`（`setHalfDose(off)` 清理 `PENDING_HALF_VALUE`）；首页半量开关（`KegelApp.kt:269-273`）。
  影响：FR-002 安全闸要求「无症状恢复、用户再确认安全说明方可恢复低量」；当前次日半量生效当天，用户翻转开关即恢复满量，无任何症状/安全确认流。
  建议：pending 半量生效日禁用开关或关闭前弹安全确认说明；至少在 Plan/HANDOFF 记录该取舍并经用户认可。

- **P2-3｜2/10/10 兜底仍建议 L1（上轮原 P2-4，未处理）**
  位置：`app/src/main/java/com/orca/kegel/domain/Assessment.kt:51`；`DomainTest.kt:557-558` 固化该行为。
  影响：与 V2.0 AC-07「2/10/10 无 L1 建议、显示教育/求助」不符（V1 既有行为，本版 AC 重写后成偏差）。
  建议：兜底改 `null` 并复用结果页 veto 分支显示教育/求助文案，同步改测试。

## 五、七查小结

- DEV_BASELINE 一致：按 `PRODUCT_PLAN_V2.0 + 修正令 B2`；本轮修复均未引入越权放行。
- Requirement 覆盖：FR-005 第 5 条（摸底扣额）、FR-002 安全闸（2 小时间隔、疲劳即停＋半量）已落地；残留缺口均已降级 P2。
- DoD 达成：AC-56/57/61 的实现子项达成；真机/UI/听感证据留待 qa（AC 状态 OPEN 正确）。
- Diff 越界：无（改动面与返工单一致：AssessmentScreen/AppViewModel/AppRepository/Plan/History/TrainingService/TrainingScreens/KegelApp/strings/DomainTest）。
- 回归影响：暂停/焦点/锁屏/安全出口/迁移/主题语言无回归；测试 43 全绿实测复核。
- P0-P2 分级：如上（P0=0 / P1=0 / P2=3）。
- 可回滚性：未提交，`git checkout` 可整体回退；新增 DataStore 键（assessment_quota_*、pending_half_*、startedByDay、doseVersion）均向后兼容。

## 六、结论

**PASS**（P0=0，P1=0，P2=3）。上轮 P1-1（摸底扣额）、P1-2（2 小时会话间隔）、P1-3（疲劳即停＋次日半量）均已真修并有独立测试；P2 三条（摸底中途退出旁路、疲劳半量开关解除、2/10/10 兜底）不阻塞进 QA，建议随后修。**可进 qa 真机验收。**
