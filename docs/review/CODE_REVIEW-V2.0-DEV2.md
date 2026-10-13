# CODE REVIEW｜DEV-V2 剂量体系（TASK-020）

- Task: TASK-020（builder，DEV-V2 六级剂量矩阵＋确定性额度换算＋会话次数分离）
- Commit: 未提交（`git diff` 工作区，2026-10-13 复核时点）
- Reviewer: code-reviewer（codebuddy/glm-5.3-flash，独立 Session，只读复核）
- 需求真源: `docs/pm/PRODUCT_PLAN_V2.0.md`（FR-002/003/004/005/008 剂量与组数节、AC-54..61）＋用户修正令 B2（取消一切外部许可门槛）
- Result: **打回（FAIL）** — P0=0，P1=3，P2=4。三条 P1 均为本轮保留需求的实现缺口，修完复检后方可进 QA。

> 审查范围：只读 diff 与相关上下文；未改任何业务代码；禁项（风格/命名/依赖）未涉及。

## 一、复核重点逐条结论（附证据）

### 1. 矩阵与公式自洽 — 通过
- `Levels.kt:85-91` 六级数值与 V2.0 满量矩阵逐格一致（H/R/E/fastSec/r/F/N：3/6/15/1/4/15/3 … 10/10/120/1/2/120/6）；`dailyTotal=E+F` 守恒且 ≤240（`Levels.kt:66`）。
- `gE/gF=ceil(·/10)`：`LevelRules.groups`（`Levels.kt:113-115`）整数上取整正确。
- 时长公式 `T=6+e×(H+R)+f×(1+r)+30×(gE+gF−1)`：`sessionSeconds`（`Levels.kt:121-130`）与 Plan §时长与组数一致；实测 L1=106、L4=321、L6=556，与表逐格相符（单测 `standardSessionSeconds_matchV2Table` 用独立硬编码期望值，非"断言等于实现"）。
- `B=min(D,240)→E_B=floor(B×E/D)、F_B=B−E_B`、半量按类 floor 折半、`Q=min(Σ,40N)`、`E_Q=floor(Q×E_H/(E_H+F_H))`：`Quota.daily`（`Plan.kt:127-144`）逐条对上 FR-005 第 1/2 条；半量 L2=12+10=22、L6=60+60=120 有测试。
- 频次降量示例：L6 选 3=120/选 6=240/选 1=40、L2 选 2=23+22 分配（E=12/12，F=11/10）均有独立断言（`DomainTest.kt` l6_sessionSplit、l2_twoSessions），与 Plan §FR-005 表一致。
- 进阶桥接路径（30→45→…→240 阶段日额、7 完成日）未实现——按 B2「取消分期解锁、当日即可满量」判定为**符合修正令**，AC-60 视为被 B2 覆盖，不计缺陷。

### 2. 不可越界（单会话≤40 / 组≤10 / 日≤240）— 主链通过，摸底旁路缺口（P1-1）
- 组合链路被强制：`Quota.build`（`Plan.kt:160-196`）按时段原始分配与双维余量取小；`Q=min(·,40×N)` 保证单时段 ≤40；`forSession` 越界返回 0/0 → Blocked（第 7 会话无入口，有测试）。
- 组 ≤10 由 `ScriptBuilder` 拆组保证（`Plan.kt:231-236`）；日 ≤240 由 `min(D,240)`＋按类型余量取小保证。
- **旁路缺口**：摸底实际发生的收缩（保持测试 ≤10 秒＋逐轮耐力 ≤10 次，最多约 20 次真实收缩）完全不扣当日额度——`AssessmentScreen.kt:85` 仅 `saveAssessment`，全链无摸底→SessionRecord/扣额路径。违反 FR-005 第 5 条「摸底扣实际额，余量不足提示次日」。见 P1-1。
- `startLevelPreview`（`AppViewModel.kt:113-127`）绕过冻结的隐患见 P2-4（当前无调用方，未成现实越界）。

### 3. 额度扣减口径 — 会话内通过，两处偏差（P1-1 / P2-5）
- 「进入即扣、中止不返还」：`TrainingService.countEnteredSteps`（`TrainingService.kt:241-256`）在进入 HOLD/QUICK_TIGHTEN 步即计 `enduranceStarted/fastStarted`，结束时随记录落库（`TrainingService.kt:349-363`）；`AppState.usedE/usedF` 取 `maxOf(started, reps)`（`AppRepository.kt:64-69`），中止记录照扣（有测试 appState_abortDoesNotReturnQuota）。会话幂等成立。
- 跨类型不互转：`e=min(e_i,rE)、f=min(f_i,rF)` 严格按类取小（`Plan.kt:181-183`），有专项测试 build_usesTypeRemainingAndNeverMovesFastToEndurance。
- **跨午夜按结束日扣额**：`TrainingService.kt:350` `epochDay = LocalDate.now()`（结束时刻落日）。FR-005 第 6 条要求「每次按发生日扣额」。builder 自陈风险点——**判定：算不一致**（措辞与实现明确不符），但边界场景量有界、不破坏 240 上限，定 P2-5。
- 摸底扣额缺失并入 P1-1。

### 4. 30 秒组间休息 — 通过（显示误差属可接受近似）
- 组间/跨类统一 30 秒完全放松（`LevelRules.GROUP_REST_SEC=30`，`Plan.kt:275-283` restStep），含耐力→快速边界（tailCue=FAST_INTRO）。
- 不可缩短：`SessionScript.extendCurrent` 仅允许 REST 相位、仅加不减（`Plan.kt:336-345`）；服务端仅在 RUNNING 态受理（`TrainingService.kt:200-203`）；无任何缩短路径。UI 仅在 REST 且未暂停时出现「再休息 5 秒」（`TrainingScreens.kt:160-165`）。
- 显示误差：`estimatedSeconds` 按固定 30 秒/间隙静态估算，延长后实际时长超出显示值；文案已声明「不含组间休息延长」（strings `home_plan_duration`）——**属可接受近似，不计缺陷**。

### 5. 状态机与中断 — 无回归
- 对比 `git show HEAD`：暂停/恢复/提前结束/焦点丢失/锁屏低调模式逻辑未改；新增 `ExtendRest` 命令仅在 `stage==RUNNING` 生效，暂停期间不推进不计数，`countedUpTo` 游标保证恢复后不重计（`TrainingService.kt:241-256,270`）。
- HOLD 步合并原「收紧 2 秒」步后，语音节奏改为 lead=TIGHTEN＋（H≥5 时）1 秒处补「保持」＋末 3 秒 3/2/1（`Plan.kt:238-248`），符合「H 含收紧起始、不另加 2 秒」；无 cue 重叠路径。
- 安全出口三态（PAIN/URINARY/RELAX_FAIL）语义未变；但疲劳出口缺失见 P1-3（V1 既有缺口，本版 AC-31/61 明确要求）。

### 6. 持久化与迁移 — 通过（一处追溯性折扣 P2-6）
- 旧记录解码：新增字段均有默认值，kotlinx.serialization 对缺省字段回退默认（`History.kt:24-30`）；`decodeHistory` 失败回退空表不崩溃（`AppRepository.kt:186-188`）；`usedE/usedF` 用 `maxOf(started, reps)` 使 V1 旧记录按完成次保守计额，**旧 40 上限历史不被追溯改写**。
- 旧设置迁移：`reminderTimes` 回退读旧键 `daily_frequency`（`AppRepository.kt:197-199`），不丢用户偏好；`sessionCount` 新键缺省=AUTO 按等级默认。
- 半量从 UI 临时态改持久化（HALF_DATE/HALF_VALUE），仅冻结日生效（`AppRepository.kt:96-98`）；当日已启动不可关半量（不抬额）符合 FR-005 第 6 条。
- **折扣**：V1 旧记录解码后 `doseVersion` 回退为 2，与 V2 记录不可区分（`History.kt:30` 注释自认「回退 2 表示不追溯」）——AC-58「doseVersion 可追溯」打折扣，P2-6。

### 7. 文案与合规 — 通过
- 中英新增键齐备且对称（`values/strings.xml`、`values-en/strings.xml` 同步增改，无缺键）；会话次数/提醒分离、组间休息、扣额不返还均有双语说明。
- 合规文案照 V2.0 §FR-002 落「关于」页（`settings_compliance`，zh/en），含「240 是产品计数上限，非医学安全保证」「不能保证治疗尿失禁、ED、PE」；无疗效承诺、无把产品上限说成医学保证的表述。
- 未开放原因/降量原因文案（`home_reduced_note`）如实描述双维下调。

### 8. 测试有效性 — 36 例基本有效，覆盖缺口与缺陷对应
- 36 个用例确认；关键不变量（矩阵逐格、公式表值、频次拆分、双维取小、中止不返还、冻结只收不放、组拆分、30 秒休息、extendCurrent 仅 REST、摸底阈值）均有**独立硬编码期望值**，未发现「断言等于实现」的假绿。
- 覆盖缺口与 P1 对应：摸底扣额、2 小时会话间隔、疲劳出口均无测试（因均未实现）。
- `build_neverExceedsSessionAndDailyCaps` 中 `p.enduranceReps<=40 && p.fastReps<=40` 断言偏弱（应≤单时段分配值），不影响结论。

## 二、Findings

### P0（0 条）
无。额度三上限（组/会话/日）在数据流上成立，冻结「只收不放」与按类取小正确，无越权放行 240 的路径。

### P1（3 条，进 QA 前必须修）

- **P1-1｜摸底扣额未实现（FR-005 第 5 条；AC-57）**
  位置：`app/src/main/java/com/orca/kegel/ui/AssessmentScreen.kt:85`（仅 `vm.saveAssessment`）；全仓无摸底→额度扣减路径。
  影响：摸底实际收缩（≤10 保持＋≤10 逐轮耐力，快速滑块自报不扣属正确）不占当日双维额，240 上限可被摸底额外突破约 20 次/日；「余量不足提示次日」缺失。
  建议：摸底保持测试开始计时与每轮耐力进入时按次累计，结束时写入一条不计时长、不计完成会话的额度记录（`enduranceStarted` 口径，`completed=false`、不计入统计与连续天数），启动摸底前按当日余量校验，不足提示次日。

- **P1-2｜会话间隔≥2 小时未实现（FR-002 安全闸；AC-56）**
  位置：`app/src/main/java/com/orca/kegel/data/AppRepository.kt:74`（`nextSessionIndex` 仅按当日记录条数推进）；全仓无 7200 秒/间隔检查。
  影响：同一时段额用尽前可连续紧贴启动下一节，违反「前一会话结束未满 2 小时不接着启动下一节活跃量」；恢复不能保证的安全前提失守。
  建议：记录各 session 的结束时间戳（`timestampMs` 已有），启动前校验当日上一条记录距今 ≥2 小时，不足则 Blocked 并给出可执行提示（延后/休息）。

- **P1-3｜疲劳即停（SAFETY_ABORT_FATIGUE）与疲劳后半量缺失（FR-002 安全闸；AC-31/61；B2 明确保留「疲劳/疼痛即停与半量降级」）**
  位置：`app/src/main/java/com/orca/kegel/domain/Plan.kt:41`（枚举仅 PAIN/URINARY/RELAX_FAIL）；`app/src/main/java/com/orca/kegel/ui/TrainingScreens.kt:265-267`（安全出口无疲劳项）；无「无症状恢复后下次最多半量」逻辑。
  影响：用户无法以「疲劳」一键停止并落库，疲劳次日强制半量（`floor(n/2)` 整日双维）不生效——本轮保留的安全要求未交付。
  建议：枚举补 `FATIGUE`＋strings 双语文案＋安全出口选项；落库 `SAFETY_ABORT_FATIGUE` 后次日自动置半量冻结（复用 `halfDoseDate/halfDoseValue`，仅次日生效）。

### P2（4 条，不阻塞进 QA，建议随后修）

- **P2-1｜`startLevelPreview` 潜在冻结旁路**：`app/src/main/java/com/orca/kegel/ui/AppViewModel.kt:113-127` 用 `effectiveSessions(level)`＋原始 `level` 而非 `dayLevel/daySessions`，一旦被 UI 接线即绕过「当日只收不放」。当前无调用方（死代码）。建议改为 `dayLevel/daySessions` 口径或直接删除。
- **P2-2｜跨午夜按结束日扣额**：`app/src/main/java/com/orca/kegel/audio/TrainingService.kt:350`，与 FR-005 第 6 条「按发生日扣额」不一致（builder 自陈；判定为不一致但低风险）。建议按步进入时刻的 epochDay 分桶扣额，或先在 Plan/HANDOFF 记录偏差并经用户认可。
- **P2-3｜旧记录 doseVersion 不可追溯**：`app/src/main/java/com/orca/kegel/domain/History.kt:30`，V1 旧记录解码回退 2，与 V2 记录无法区分，AC-58「doseVersion 可追溯」打折扣。建议解码时以「字段缺失→doseVersion=1」标注旧记录。
- **P2-4｜2/10/10 摸底回退建议 L1**：`app/src/main/java/com/orca/kegel/domain/Assessment.kt:52`（`return Level.L1` 兜底），与 V2.0 AC-07「2/10/10 无 L1 建议，显示教育/求助」不符（V1 既有行为，本版 AC 重写后成偏差；单测还把该行为固化）。建议兜底改 `null` 并在结果页显示教育/求助文案。

## 三、七查小结

- DEV_BASELINE 一致：按 `PRODUCT_PLAN_V2.0 + 修正令 B2` 实现，许可门槛类条款（C≤45/专业审查/分期解锁）正确移除，保留项基本落地（疲劳除外）。
- Requirement 覆盖：剂量矩阵/公式/额度/冻结/半量/组拆/文案/迁移覆盖；缺口＝P1-1/2/3。
- DoD 达成：DoD-02/04 的额度与迁移部分达成；AC-56/57/61 相关子项缺口如上。
- Diff 越界：无（改动面与派工单一致；进阶路径未实现属 B2 授权范围）。
- 回归影响：暂停/焦点/锁屏/安全退出/迁移无回归。
- P0-P2 分级：如上。
- 可回滚性：未提交，`git checkout` 可整体回退；新增 DataStore 键向后兼容。

## 四、结论

**FAIL**（P0=0，P1=3，P2=4）。矩阵与额度主链实现正确、测试独立有效，但 P1-1（摸底扣额）、P1-2（2 小时会话间隔）、P1-3（疲劳即停＋疲劳后半量）为本轮保留需求的实现缺口，须修复并补测试后复检，方可进 QA。
