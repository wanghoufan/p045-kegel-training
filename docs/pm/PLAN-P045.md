# PLAN｜P045 男性盆底肌语音训练器（Phase2 开发）

- DEV_BASELINE：`Product Plan V1.1`（根目录需求真源）＋`docs/design/PRODUCT_RULES_APPROVED.md`（OD-01..07 与 2026-10-07 V3 补充决议）＋原型 `PROTOTYPE-V4` 交互基线
- CHANGE_REQUEST：NONE
- 执行模式（用户 2026-10-09 令）：**唯一开发实现者直做**，不再派多角色；但验收证据仍逐条落 `docs/qa/产品验收追踪矩阵.md`，不得以「代码已写」判通过。
- 取证与验收口径：34 条 AC（AC-001..026＋AC-046..053）以真机/截图/日志/命令输出为证据。

## Product Goal

用户戴耳机、几乎不看屏幕，即可完成与自身训练能力匹配的「耐力＋快速」男性盆底肌训练，并看到长期记录与能力变化。

## Current Stage

- Stage ID：`DEV-V1`（Android 真机可安装 MVP）
- Goal：产出可安装到目标真机（Redmi Note 11T Pro / API 34）的 APK，跑通「摸底→选级→今日训练→记录→设置」主路径，全程语音引导，主题/多语言达 APP 基础规范。

## Stage P0（缺任一条不算完）

- [ ] APK 可安装并在目标真机启动；minSdk 31 / targetSdk 36
- [ ] 摸底五项（可选、可跳过）：动作控制、耐力保持秒(1–10)、耐力次数、快速次数、放松质量；阈值建议＝短板约束＋一票否决
- [ ] 今日训练：按当前等级自动串联耐力＋快速，用户不需设秒数/次数；一键开始（无「准备等待」）
- [ ] 训练中暂停/恢复（原相位原剩余）/提前结束；来电或音频焦点丢失自动暂停
- [ ] 日额度 ≤40 次收缩；频率 1/2/3（选 3 自动降量）；连续 6 日给恢复日/半量提示
- [ ] 小语提示音＋低调模式（无声字幕＋相位倒计时），公共场合不强制外放
- [ ] 安全出口分类（疼痛 / 排尿排便困难 / 无法放松）→ 本次立即停止且不可恢复
- [ ] 记录与统计：耐力/快速分列、训练日与次数、等级变化、最长有效保持
- [ ] 设置：提醒频率、EHS 可选个人自评（默认关、本地、可单独清除、不动等级）
- [ ] 主题三态 LIGHT/DARK/SYSTEM（默认 SYSTEM）＋ zh-CN/en（默认跟随系统、不支持回退 zh-CN）＋重启持久化＋切换不丢训练状态
- [ ] 34 条 AC 逐条取证

## 已批准规则（实现契约，取自 UX_CONTRACT.json 批准值）

六级单值矩阵（耐力收紧/完全放松/耐力次数 ｜ 快速收紧/快速间隔/快速次数）：

| Level | hold | rest | E | fast | fast_rest | F |
|---|---:|---:|---:|---:|---:|---:|
| L1 | 3 | 6 | 5 | 1 | 4 | 5 |
| L2 | 4 | 6 | 6 | 1 | 3 | 6 |
| L3 | 5 | 5 | 8 | 1 | 3 | 8 |
| L4 | 6 | 6 | 8 | 1 | 2 | 10 |
| L5 | 8 | 8 | 10 | 1 | 2 | 10 |
| L6 | 10 | 10 | 10 | 1 | 2 | 10 |

初始等级建议阈值（短板约束，全部达标才取该级，从上往下取最高命中；包含「完全放松」与「动作控制」前置否决）：

| Level | 耐力秒≥ | 耐力次数≥ | 快速次数≥ |
|---|---:|---:|---:|
| L1 | 3 | 5 | 5 |
| L2 | 4 | 6 | 6 |
| L3 | 5 | 8 | 8 |
| L4 | 6 | 8 | 10 |
| L5 | 8 | 10 | 10 |
| L6 | 10 | 10 | 10 |

其他锁定值：日上限 40 次收缩；频率 3 → `E=min(E,6), F=min(F,13-E)`；半量 → 各 `floor(n/2)` 且 ≥1；组合训练剩余额度 <2 时不启动；等级为单主 Level L1–L6，随时可改，不被测试/推荐锁死；摸底结果仅作默认建议；14 个完成日后提示一次重测（可忽略）；EHS 独立于等级。

## In Scope

- 原生 Android（Kotlin + Jetpack Compose + Media3），**中文/英文**，本地离线语音资产。
- 语音资产：复用 `prototype/audio-poc/app/src/main/res/raw/cue*.wav`（中文 8 段）；英文另生成。
- 真机装机、自测、逐条 AC 取证。

## Out of Scope

- ru/ko/th 翻译（仅保证架构可扩展，AC-053）；云同步/社交/排行；AI 治疗师；外接硬件；医学诊断口径。
- 不改原型 `prototype.html`（除后续如用户要求）；不删 `prototype/audio-poc`（用户令保留）。
- 不 push 远端；不改 ORCA 母版。

## 技术复用矩阵（Reuse Audit 结论）

| 能力 | 决策 | 候选 | 理由 |
|---|---|---|---|
| 音频播放/后台/锁屏播控 | REUSE | androidx.media3 exoplayer+session 1.11.1 | 已在目标真机验证锁屏/焦点/通知播控（PRE-02 证据） |
| 播放服务骨架（MediaSessionService＋焦点处理） | ADAPT | `prototype/audio-poc` PlaybackService/FocusGrabber | 样机骨架可用，业务化改造 |
| 中文提示音资产 | REUSE | audio-poc `res/raw/cue01..08.wav` | 已录制/合成，离线、节奏可控 |
| 英文提示音资产 | BUILD | macOS `say` 合成（构建期生成） | 无现成资产；V1 只需 zh/en |
| UI 框架 | REUSE | Jetpack Compose + Material 3 1.3.0 | 官方；主题三态用 Material3 colorScheme |
| 主题三态与持久化 | REUSE | Material3 + DataStore(Preferences) | 官方组件，不自研 |
| 多语言 | REUSE | Android 官方 `res/values`+`values-en` + Per-app language | 官方方案；回退 zh-CN |
| 业务数据持久化 | REUSE | androidx.datastore + kotlinx-serialization-json | 无 schema 迁移负担，满足本地记录需求 |
| 等级/额度/状态机规则引擎 | BUILD | 纯 Kotlin（可单测） | 无现成库；必须严格对齐批准矩阵 |
| 旧原型 HTML 模拟器 | REVIEW_FOR_REUSE | `docs/design/prototype/prototype.html` | 仅作行为参照（已验证交互），不搬其代码 |

## Task Breakdown

| Task ID | Priority | Role | Status | Notes |
|---|---|---|---|---|
| T1 | P0 | builder | TODO | 工程脚手架：Gradle/AGP 8.13/Kotlin 2.1.20/Compose/Media3/DataStore；`app/`；主题三态＋i18n 骨架 |
| T2 | P0 | builder | TODO | 领域层：等级矩阵、阈值建议、计划生成、额度、状态机（纯 Kotlin＋单测） |
| T3 | P0 | builder | TODO | 持久化：DataStore 模型（设置/等级/摸底/会话/EHS）＋重启恢复 |
| T4 | P0 | builder | TODO | 音频：cue 资产（zh 复用/en 生成）＋ MediaSessionService ＋ 节奏调度＋低调模式 |
| T5 | P0 | builder | TODO | UI：今日/摸底/能力/训练/暂停/结束/记录/设置/EHS/各级标准 |
| T6 | P0 | builder | TODO | 安全与中断：焦点丢失/来电暂停、安全出口分类、恢复限制 |
| T7 | P0 | qa（同人） | TODO | 真机装机＋自测＋34 条 AC 取证落追踪矩阵 |
| T8 | P1 | 编排者 | TODO | docs/HANDOFF/账本/经验一句话 收尾 |

## 需求→任务追踪（requirements-traceability）

| AC | 任务 | AC | 任务 |
|---|---|---|---|
| AC-001..005（摸底） | T2,T5,T3 | AC-016..018（语音） | T4 |
| AC-006..008（等级） | T2,T5 | AC-019..021（历史） | T3,T5 |
| AC-009..011（耐力/暂停） | T2,T5,T6 | AC-022..023（重测/升级） | T2,T3 |
| AC-012..013（快速/分列） | T2,T3,T5 | AC-024..026（安全） | T5,T6 |
| AC-014..015（今日训练） | T2,T5 | AC-046..053（主题/i18n/语音回退） | T1,T3,T4,T5 |

## Risks

- 英文语音资产靠机器合成，音质一般 → V1 可接受，AC-052 以「可复现＋失败回退」为判据。
- 前台服务类型 `mediaPlayback` 在 API 34 的限制 → 沿用样机已验证配置，需声明权限并实测。
- 计时精度：按 cue 结束＋计时器双保险推进相位，避免语音延迟拖节奏。
- 一次性做完 34 条 AC 取证工作量大 → 先保证 P0 主路径真机可跑，再补齐逐条证据。

## Human Decisions Needed

- 无。如实现中出现与批准规则冲突的必要取舍，立即停下来集中提问。