# HANDOFF｜交接（暂停/恢复用，先读我）

> 旧版字段（governance-state / Evidence / Human Gate / Promotion / Dispatch ID）已废弃，不填。

- Captured at（YYYY-MM-DD HH:MM）：2026-10-07 22:30
- PROJECT_PHASE：PLAN（用户 2026-10-07 纠偏令：正式状态保持 PLAN；尚**未**发出正式 `第二阶段，开发`）
- PLAN_VERSION：以用户 Product Plan V1.1 为需求真源（`2026-10-07 - Mac Mini - ChatGPT - 男性盆底肌语音训练器Product Plan-计划 - V1.1.md`；副本 `temp/用户草稿-Product Plan V1.1.md`）
- PLAN_READINESS_SCORE：（设计管线阶段不适用）
- PLAN_GATE：（设计管线中，状态以下方 Design 字段为准）
- DEV_BASELINE：（未建立；管线跑到 Human 3 才建）
- CHANGE_REQUEST：NONE
- Stage ID（本阶段叫什么）：V1.6 设计管线｜目标＝跑到 **Human 1（Direction Selection）**
- Design 状态：`DIRECTION_SELECTED`（A）→ `UX_INTERACTION_PASS` → `interactive-product-prototype` 完成五槽；runtime=`PARTIAL_PENDING_AUDIO`（TM 补真浏览器：首次摸底全流程+recovery＋console 0 错；训练执行态因验证浏览器无系统音频记 PENDING，要求带音频环境重跑＝Human 2）
- Prototype：`docs/design/prototype/prototype.html`（自包含可点击模拟器，覆盖核心流程＋数音/暂停/恢复/来电/安全退出/EHS）｜`PROTOTYPE_MANIFEST.json`｜`PROTOTYPE_SCREENSHOT_INDEX.md`｜`PROTOTYPE_QA.md`｜`runtime-evidence/`（runtime 证据＋10 张状态样例图）；本地预览 http://127.0.0.1:8901/prototype.html
- 产品规则已结案：`docs/design/PRODUCT_RULES_APPROVED.md`（OD-01..07 用户决议＋六级单值矩阵/五维阈值/停止规则，出处 `docs/pm/PRODUCT_PLAN_V1.4.md`）
- Design 产物：`docs/design/SPEC.md`（behavior-only）｜`RESEARCH_PACKAGE.json`（DESIGN_RESEARCH=PASS，10 源/15 观察）｜`DIRECTION_SET.json`（A/B/C，8 轴差异，selected=null）｜`directions/{A,B,C}/index.html`＋`preview.png`｜`index.html`（比较入口）｜`HUMAN1-方向选择.md`｜`research/assets/`（G动、Squeezy 参考图）｜`TECH-REUSE-POLICY.md`
- 管线：Product Plan → behavior-only SPEC → design-reference-research → visual-direction-exploration → **Human 1** → ux-interaction-contract → interactive-product-prototype → Human 2 → design-freeze → **Reuse Audit（Prototype Approval 后、SDD 前）** → SDD PLAN/TASK（必带技术复用矩阵）→ requirements-traceability → DEV_BASELINE → Human 3 → DEVELOP
- 技术复用政策（用户 2026-10-07 令，全流程）：优先级＝官方框架/SDK → 主流成熟库 → License 清晰的成熟开源 → 用户旧项目代码 `REVIEW_FOR_REUSE`（先审后用）→ 都不适合才 `BUILD`；决策枚举 `REUSE / ADAPT / REVIEW_FOR_REUSE / BUILD`；市场/官方复用优先→自有代码复审其次→自建最后；详见 `docs/design/TECH-REUSE-POLICY.md`。不因本政策提前开发；技术 spike 仍只是 feasibility evidence。
- 剩 P0（没完的才列，多一条都不行）：①behavior-only SPEC；②RESEARCH_PACKAGE（真实竞品/参考证据）；③DIRECTION_SET（3 个真正不同的方向＋可看预览）；④Human 1 材料交用户选 A/B/C。
- 当前 Task（正干到哪）（累计打回 n/2）：TASK-010 V1.6 设计管线至 Human 1（进行中，0/2）
- 执行链/Session（可选）：见 docs/model/DISPATCH-LOG.jsonl。
- 模型分工变更（2026-10-07，用户令）：母版真源表 T25——所有 `codex/gpt-6-sol` → `codex/gpt-6.1-sol`（planner 行已改；senior-expert 本就是 6.1）。真调 `codex exec -m gpt-6.1-sol` 回 `pong` 通过；马上生效，后续派工一律用 `codex/gpt-6.1-sol`（历史调用档案行不改写）。
- 未闭环评审意见：无。
- 产品验收：未到。
- 用户签收：未到（发布类型＝首次发布，收尾需签收）。
- **TEMP_FEASIBILITY_EVIDENCE（保留，非进度）**：`docs/qa/PRE-02-技术样机/`（48 件证据，含 S1/S3/S4 录屏）＋`prototype/audio-poc/`（原生 Kotlin+Media3 音频样机）＝ 2026-10-07 Android 音频技术 spike；**不代表** Prototype/Design Freeze/SDD/Phase2 完成，不计入正式进度；仅留待 SDD 技术选型决定 KEEP/REUSE/REMOVE。见该目录 `TEMP_FEASIBILITY_EVIDENCE.md`。
- 早前 ORCA 模板 Phase1 产物（`docs/pm/PRODUCT_PLAN_V1.0..V1.4.md`、`docs/review/RESEARCH_REVIEW-V1.0-R1..V1.3-R4.md`）保留在库、非本管线真源；需求真源以用户 V1.1 草稿为准。
- Human 2 反馈（2026-10-07，用户实点，未批准待改）：①摸底**可选**，可自选 L1–L6 直接开始，不强制；②训练放**首页**、一键开始；③摸底每步讲清做什么/点哪/反馈＋计数/第N/5项＋"五维"具象；④今日训练可见阶段/进度/秒/次数效果。
- **Prototype V3 已改完**（R1–R5 落地）：①摸底·耐力次数＝"一次按刚测保持秒数收→完全放松，两次间完全放松＋休息约5秒标准，点一次记一次 X/10、可结束本项"，不脑内盲数；②今日/能力页有「各级标准」入口展示 L1–L6 单值矩阵给目标；③"开始今日训练"始终可点击（正常任何情况可开展训练，安全锁只处理真实异常）；④快速模式语音仅 `紧`/`松`两字、节奏不重叠，完整提示"完全放松"→"放松"；⑤等级自由选择（随时可改 L1–L6、不被测试锁死）、摸底可选独立、最近摸底结果持久化为默认建议（可改）、界面独立显示"上次摸底 L?/当前选择 L?"。UX V1.3 PASS（14f/17s/72i/62st），静态 133 项过；runtime 仍 PENDING（验证浏览器无音频，以你实点为准）。
- 下一步（Next Single Action）：用户桌面 Chrome **刷新复点 Prototype V4**（`准备10秒` 已删、点开始立即训练）→ 批准（Human 2）或再提调整 → 批准后 Design Freeze → Reuse Audit → SDD。
- 人要拍什么板（列出来问，不问不许开工）：Human 2＝用户实点 Prototype V4 并批准。
- **治理修复任务《P045 worktree 错位修复＋铺包防呆》收单并排队**（2026-10-07；任务书自定"本阶段开发完成后才执行"）。现状已核：铺包全落在 `1.Active/main`（worktree/分支 wanghoufan/main），主仓 `045-ing-凯格尔训练`（main）仅空壳＋未跟踪 V1.1 草稿；两 worktree 同 commit 2914c25。
  - **已做（可回退保护）**：把 `1.Active/main` 全部未提交内容备份至 `045-ing-凯格尔训练/temp/worktree-backup-2026-10-07/`（137MB）。
  - **待用户明确指令（红线）**：A2 commit（分支 wanghoufan/main）→ A3 merge 入 main → A4 `git worktree remove 1.Active/main`（不可逆）→ A5 重建 `.worktree/main`；禁 push/merge 远端写入除非另令。
  - **Task B（母版防呆）**：改母版规范（项目根 `.git` 须为目录；worktree 路径须在项目内；铺包目标=项目根、禁相对路径 worktree add；铺包后 ls 校验）；母版红线流程：先出融合章节草案→用户审→写入→`_sync-packages.py` 同步两包→补概览→`check-sync.sh` SYNC-OK→经验一句话。
- 约束（用户令）：不删技术验证；不重开项目；不重跑 Skill benchmark；不改 ORCA 母版；不 git push。
- 收尾记一笔：待收尾派 neat-freak。

## 恢复读盘（全体系唯一顺序，别乱）

1. AGENTS；2. 角色卡；3. 根 `USER_MODEL_OVERRIDE.md`；4. 本 HANDOFF；5. 根 `经验一句话.md`；6. 任务目标放最后。
冲突才扩大读。
