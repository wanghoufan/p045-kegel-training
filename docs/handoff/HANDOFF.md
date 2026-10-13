# HANDOFF｜交接（暂停/恢复用，先读我）

> 旧版字段（governance-state / Evidence / Human Gate / Promotion / Dispatch ID）已废弃，不填。

## 小交接快照（2026-10-10 开发暂停｜恢复先读这一段）

### 一、当前进展（到哪了）
- 阶段：`PROJECT_PHASE=DEVELOP`，执行模式＝**唯一开发实现者直做**（角色/Gate 不取消，证据落 `docs/qa/产品验收追踪矩阵.md`）；Stage＝**`DEV-V1`**。
- 产品：Android 盆底肌语音训练 App，包名 **`com.orca.kegel`**（用户 2026-10-10 指定正式版＝主仓 `main` 这一套）；功能已全部实现（今日/摸底/能力/训练/暂停/结束/设置/EHS/各级标准＋主题三态＋zh-en）。
- 验收：**34 条 AC 全 PASS**（关键 7 条 AC-046..052 全 PASS），逐条证据＋截图见 `docs/qa/产品验收追踪矩阵.md` 与 `docs/qa/evidence-P045/`。
- 构建：`./gradlew assembleDebug testDebugUnitTest` = **BUILD SUCCESSFUL**（21 单测绿）。
- 真机：已装 **`IN9LZTAYV4UGU4JF`**（Redmi Note 11T Pro），可正常运行；桌面 APK 副本 `~/Desktop/盆底肌训练-安装包/盆底肌训练-v1.0-debug.apk` 已刷新至本轮最新（sha256 与仓内产物一致）。
- 本轮 **Change B（摸底改版 TASK-018，留 DEVELOP）已收官**：第 2 项加正计时器；第 3 项改全程自动化逐轮引导（收紧倒计时 → **休息也倒计时** → **数完自动进下一轮，无需点击**，可点「再休息 5 秒」延长）；第 4 项改一次性滑块；摸底计时全程加**语音＋提示音**（新增 `audio/AssessmentAudio.kt` ＋ `res/raw/tick.wav`，zh/en 双份）。
- **未完成的收尾**：无——①用户已实听并复核听感类 AC-052/016/017；②**首次发布用户签收已到（2026-10-10 口令「签收」）**，整链 `chain_status` 已转 `ACCEPTED`，回执见 `docs/qa/产品验收追踪矩阵.md`「首次发布签收回执」。

### 二、下一步任务（2026-10-10 签收后更新）
1. ~~实听复核 AC-052/016/017＋首次发布签收~~：**已完成**（用户口令「签收」→整链 `ACCEPTED`）。
2. ~~收尾：commit＋展示层同步~~：**已完成（TASK-019，2026-10-10 用户令三步）**——①jcp 小清理；②双语 README 重写＋About 三格＋push（`9743c05`＋`.project.yaml` 除名提交）；③**仓库已转公开**、**Release v1.0 已发布并挂 APK**（`pelvic-trainer-v1.0-debug.apk`，debug 签名已在 Release 注明，SHA-256 `0afda3e8…`）、GitHub 主页第 29 行已加、个人网站新增 `kegel-trainer` 条目＋16:9 封面（站点测试 14/14 绿、Pages 部署 success、线上绕缓存复验封面 sha256 与本地一致）。
3. （用户持令待办）治理修复任务《P045 worktree 错位修复＋铺包防呆》——**须用户明确 A2–A5 指令才动（红线）**，不执行。
4. 若用户提出新反馈：按 **Change Request** 分类——A/B 留 `DEVELOP`；C 才走受控重开。

### 三、注意事项与规矩
- **权限边界**：不改业务代码除非用户明确授权；**不 push、不擅自 commit**（无明确指令一律不做，也不上报）。
- **交付口径**：产品验收未落盘 / 关键 AC 未测 / 首次发布未签收，**不得报完工**。
- **产品口径**：摸底等计时类功能**必须内置计时/倒计时，禁用户手动计数**（严格可靠）；交互保持**简洁低复杂度**，拒绝冗余流程。
- **红线**：不删技术验证件；不碰 secrets；不删用户数据/不可逆删除；不动生产/他人项目；不改 ORCA 母版。
- **本机操作**：桌面/外部目录写入需 `dangerouslyDisableSandbox`（Desktop 在沙箱白名单外）；构建/装机器命令也需关沙箱。环境：`JAVA_HOME=$HOME/android-toolchain/jdk-17.0.20.1+1/Contents/Home`、`ANDROID_HOME=$HOME/android-toolchain/sdk`。
- **时钟说明**：本机 `date` 读数与部分历史记录不一致（见下 `Captured at` 注），以本次刷新为准。

---

- Captured at（YYYY-MM-DD HH:MM）：2026-10-10 08:20（本机 `date` 实测；前次记录 15:20 疑为本机时钟偏差，非实际时间）
- PROJECT_PHASE：**DEVELOP**（用户 2026-10-09 00:35 明确口令 `第二阶段，开发`，Human Gate 通过）
- 执行模式（用户 2026-10-09 令）：**唯一开发实现者直做**——不再派多角色（builder/reviewer/qa/supervisor 由同一实现者承担并在文档留证）；角色与 Gate 要求不取消，证据落 `docs/qa/产品验收追踪矩阵.md`。
- PLAN_VERSION：以用户 Product Plan V1.1 为需求真源（`2026-10-07 - Mac Mini - ChatGPT - 男性盆底肌语音训练器Product Plan-计划 - V1.1.md`；副本 `temp/用户草稿-Product Plan V1.1.md`）
- PLAN_READINESS_SCORE：（设计管线阶段不适用）
- PLAN_GATE：（设计管线中，状态以下方 Design 字段为准）
- DEV_BASELINE：**`PRODUCT_PLAN_V1.1 + PRODUCT_RULES_APPROVED`**（2026-10-09 锁定；实现契约见 `docs/pm/PLAN-P045.md` 的六级单值矩阵与阈值表）。Plan 变更只走 Change C。
- CHANGE_REQUEST：**B（2026-10-10 摸底测试收紧）**——用户指出摸底计时不严。按定义（局部功能变化、更新局部 Requirement/DoD）属 B，**留 DEVELOP，不召 Planner、不开 Change C**。改动：摸底第 2/3/4 项交互重做，领域逻辑与阈值未动。
- Stage ID（本阶段叫什么）：**`DEV-V1`｜Android 真机可安装 MVP**（计划书 `docs/pm/PLAN-P045.md`）
- Design 状态：设计管线**到此收束**（`DIRECTION_SELECTED`(A) → `UX_INTERACTION_PASS` → 五槽原型 `PROTOTYPE-V4`）。用户 2026-10-09 未逐屏签批 Human 2，而是直接以口令推进开发；**PROTOTYPE-V4 作为交互基线**，其遗留 P1（PB-01 低调模式/ PB-02 安全否决摩擦 / PB-03 安全出口分类）已写入 DEV-V1 的 Stage P0 实现与验收。
- Prototype：`docs/design/prototype/prototype.html`（自包含可点击模拟器，覆盖核心流程＋数音/暂停/恢复/来电/安全退出/EHS）｜`PROTOTYPE_MANIFEST.json`｜`PROTOTYPE_SCREENSHOT_INDEX.md`｜`PROTOTYPE_QA.md`｜`runtime-evidence/`（runtime 证据＋10 张状态样例图）；本地预览 http://127.0.0.1:8901/prototype.html
- 产品规则已结案：`docs/design/PRODUCT_RULES_APPROVED.md`（OD-01..07 用户决议＋六级单值矩阵/五维阈值/停止规则，出处 `docs/pm/PRODUCT_PLAN_V1.4.md`）
- Design 产物：`docs/design/SPEC.md`（behavior-only）｜`RESEARCH_PACKAGE.json`（DESIGN_RESEARCH=PASS，10 源/15 观察）｜`DIRECTION_SET.json`（A/B/C，8 轴差异，selected=null）｜`directions/{A,B,C}/index.html`＋`preview.png`｜`index.html`（比较入口）｜`HUMAN1-方向选择.md`｜`research/assets/`（G动、Squeezy 参考图）｜`TECH-REUSE-POLICY.md`
- 管线：Product Plan → behavior-only SPEC → design-reference-research → visual-direction-exploration → **Human 1** → ux-interaction-contract → interactive-product-prototype → Human 2 → design-freeze → **Reuse Audit（Prototype Approval 后、SDD 前）** → SDD PLAN/TASK（必带技术复用矩阵）→ requirements-traceability → DEV_BASELINE → Human 3 → DEVELOP
- 技术复用政策（用户 2026-10-07 令，全流程）：优先级＝官方框架/SDK → 主流成熟库 → License 清晰的成熟开源 → 用户旧项目代码 `REVIEW_FOR_REUSE`（先审后用）→ 都不适合才 `BUILD`；决策枚举 `REUSE / ADAPT / REVIEW_FOR_REUSE / BUILD`；市场/官方复用优先→自有代码复审其次→自建最后；详见 `docs/design/TECH-REUSE-POLICY.md`。不因本政策提前开发；技术 spike 仍只是 feasibility evidence。
- 剩 P0（没完的才列，多一条都不行）：**无（无阻塞实现类 P0）**。Stage P0 清单见 `docs/pm/PLAN-P045.md`，逐条证据见 `docs/qa/产品验收追踪矩阵.md`（34 条 AC 全 PASS，关键 7 条 AC-046..052 全 PASS）。原两条收尾阻塞（真机安装、听感复核）已随 2026-10-10 用户签收全部闭环，无遗留。
- **真机交付（2026-10-10 已打通）**：`adb -s IN9LZTAYV4UGU4JF install -r` → **Success**；已启动运行（`topResumedActivity=com.orca.kegel/.MainActivity`，截图 `docs/qa/evidence-P045/30-realdevice-home.png`）。APK 另存桌面 `~/Desktop/盆底肌训练-安装包/盆底肌训练-v1.0-debug.apk`。仅剩 AC-052/016/017 听感需真机实听复核（功能态已 PASS）。
- 当前 Task（正干到哪）（累计打回 n/2）：**TASK-016（builder，DEV-V1 开发）PASS；TASK-017（qa，34 条 AC 自测取证）PASS；TASK-018（Change B 摸底改版）PASS**——三链均随用户 2026-10-10「签收」转 `ACCEPTED`，DEV-V1 完工（0/2 打回）。任务表 T1–T8 见 `docs/pm/PLAN-P045.md`。
- **Change B 收官（TASK-018，2026-10-10，留 DEVELOP）**：摸底 5 项中第 2/3/4 项按用户要求收紧——
  - 第 2 项「耐力保持秒数」＝**正计时器**：点「开始计时」→ 显示 `x / 10`＋「收紧并保持，跟着秒数走…」，撑不住点「撑不住了，停」记实际秒数，到 10 秒自动记录；测完按钮变「重新测」。未测时「下一步」置灰（holdSec 初值＝0）。
  - 第 3 项「耐力次数」＝**全程自动化逐轮引导**：每轮「开始本次（收紧并保持 N 秒）」→ 倒计时「收紧并保持 / x 秒」→ 数完**自动计数并进入休息** → 休息阶段**同样倒计时**（「完全放松，休息」＋大号剩余秒）→ **休息数完自动进入下一轮，无需手动点击**；N 取第 2 项实测秒数，计数上限 10；撑不住点「结束本项」提前结束。未记次时「下一步」置灰（reps 初值＝0）。
  - **休息可延长（2026-10-10 补，用户反馈「休息 5 秒不该倒计时吗？那他要休息 30 秒呢？」）**：休息默认 `ASSESS_REST_SECONDS=5` 秒并倒计时，不够可点「**再休息 5 秒**」按 5 秒一档延长（实测 `4 秒 → 连点 3 次 → 15 秒`）。
  - 第 4 项「快速次数」＝**一次性滑块选择**（1–10），不再逐次点 10 次。
  - **计时声音（2026-10-10 补，用户反馈「计时没有声音，用户每次都要盯屏」）**：第 2/3 项计时全程加语音＋提示音引导——复用训练本地语音资产（`cue_tighten`/`cue_relax`/`cue_done`/`cue_num3-2-1`，zh/en 双份）＋新增每秒 `tick.wav`；新增 `audio/AssessmentAudio.kt`（SoundPool，进摸底页预加载）。第 2 项：播「收紧」→ 每秒「滴」→ 最后 3 秒 3/2/1 → 10 秒「完成」自动记录；手动停播「放松」。第 3 项：每轮「收紧」→ 每秒「滴」、最后 3 秒 3/2/1 →「放松」＋**休息倒计时全程每秒「滴」、末 3 秒 3/2/1** → 自动进下一轮。
  - 证据：`docs/qa/产品验收追踪矩阵.md`「构建/命令证据」段摸底改版行＋8 张截图 `docs/qa/evidence-P045/31-38-*.png`；落库 `last_assessment={"holdSeconds":1,"enduranceReps":1,"fastReps":10,...}`。休息倒计时/自动下一轮/延长休息证据：截图 `docs/qa/evidence-P045/41-assess-reps-rest-countdown.png`；真机实测自动轮转达 `已记录 5/7/9 次`（自动计数＋自动下一轮有效）、休息阶段显示「完全放松，休息 / 4 秒 / 再休息 5 秒」、连点 3 次「再休息 5 秒」→ `15 秒`。
  - 验证：`./gradlew assembleDebug testDebugUnitTest` BUILD SUCCESSFUL（21 单测绿）；真机 `IN9LZTAYV4UGU4JF` `install -r` → Success，逐屏交互核对通过。领域逻辑 `Assessment.kt` 未改动。声音实测：`dumpsys media.audio_flinger` 见 `com.orca.kegel` 多条 SoundPool 音频流在播、logcat 无 `SoundPool`/`not ready` 报错；`res/raw/tick.wav` 在 APK 中为 `Stored`（未压缩，可加载）；截图 `39/40-assess-*-audio.png`。
- 完成情况（DEV-V1）：Android 工程已建（Kotlin 2.1.20/Compose/Media3 1.11.1/DataStore；`com.orca.kegel`，compileSdk 36/minSdk 31/targetSdk 36）；`./gradlew assembleDebug testDebugUnitTest` = BUILD SUCCESSFUL（21 单测绿）；APK 已装模拟器 `emulator-5556` 逐条取证（30 张截图存 `docs/qa/evidence-P045/`）。
- **用户决定（2026-10-10）：正式版＝主仓 `main`（`com.orca.kegel`，本轮"唯一实现者直做"这套）**。用户原话：副本 `main-2`（`com.p045.pelvicfloor`）「太复杂了，我不喜欢用。你的这个就很好」。→ `main-2` 降为**归档保留、不再推进**（代码与手机图标都不删）；本轮开发目标锁定主仓 `main`。
- **两套并行实现（用户 2026-10-10 令「都保留」）**：①本仓 `main`＝`com.orca.kegel`（**正式推进版**，34 条 AC 自测）；②副本 `/1.Active/main-2`＝`com.p045.pelvicfloor`（归档保留，未提交文件 210 个，仅知情不追）。备份图 `~/Desktop/两套App对比-2026-10-10/`。
- 模型分工变更（2026-10-07，用户令）：母版真源表 T25——所有 `codex/gpt-6-sol` → `codex/gpt-6.1-sol`（planner 行已改；senior-expert 本就是 6.1）。真调 `codex exec -m gpt-6.1-sol` 回 `pong` 通过；马上生效，后续派工一律用 `codex/gpt-6.1-sol`（历史调用档案行不改写）。
- 未闭环评审意见：无。
- 产品验收：**已落盘** `docs/qa/产品验收追踪矩阵.md`（34 条 AC 逐条证据＋状态；关键 7 条 AC-046..052 全 PASS；含安全/额度/低调模式补充证据表＋首次发布签收回执）。整链 `chain_status=ACCEPTED`（2026-10-10 用户签收）。
- 用户签收：**已到**（2026-10-10 对话口令「签收」；发布类型＝首次发布，签收回执落产品验收追踪矩阵；TASK-016/017/018 链状态已转 `ACCEPTED`）。
- **TEMP_FEASIBILITY_EVIDENCE（保留，非进度）**：`docs/qa/PRE-02-技术样机/`（48 件证据，含 S1/S3/S4 录屏）＋`prototype/audio-poc/`（原生 Kotlin+Media3 音频样机）＝ 2026-10-07 Android 音频技术 spike；**不代表** Prototype/Design Freeze/SDD/Phase2 完成，不计入正式进度；仅留待 SDD 技术选型决定 KEEP/REUSE/REMOVE。见该目录 `TEMP_FEASIBILITY_EVIDENCE.md`。
- 早前 ORCA 模板 Phase1 产物（`docs/pm/PRODUCT_PLAN_V1.0..V1.4.md`、`docs/review/RESEARCH_REVIEW-V1.0-R1..V1.3-R4.md`）保留在库、非本管线真源；需求真源以用户 V1.1 草稿为准。
- Human 2 反馈（2026-10-07，用户实点，未批准待改）：①摸底**可选**，可自选 L1–L6 直接开始，不强制；②训练放**首页**、一键开始；③摸底每步讲清做什么/点哪/反馈＋计数/第N/5项＋"五维"具象；④今日训练可见阶段/进度/秒/次数效果。
- **Prototype V3 已改完**（R1–R5 落地）：①摸底·耐力次数＝"一次按刚测保持秒数收→完全放松，两次间完全放松＋休息约5秒标准，点一次记一次 X/10、可结束本项"，不脑内盲数；②今日/能力页有「各级标准」入口展示 L1–L6 单值矩阵给目标；③"开始今日训练"始终可点击（正常任何情况可开展训练，安全锁只处理真实异常）；④快速模式语音仅 `紧`/`松`两字、节奏不重叠，完整提示"完全放松"→"放松"；⑤等级自由选择（随时可改 L1–L6、不被测试锁死）、摸底可选独立、最近摸底结果持久化为默认建议（可改）、界面独立显示"上次摸底 L?/当前选择 L?"。UX V1.3 PASS（14f/17s/72i/62st），静态 133 项过；runtime 仍 PENDING（验证浏览器无音频，以你实点为准）。
- 下一步（Next Single Action）：**DEV-V1 已完工（2026-10-10 用户口令「签收」，整链 `ACCEPTED`）**。后续仅两件事等用户口令：①commit 归档本轮改动（未指令不做）＋收尾链（neat-freak/experience-recorder）；②治理修复任务《P045 worktree 错位修复＋铺包防呆》（须明确 A2–A5 指令）。新反馈按 Change Request A/B/C 分类处理。
- 人要拍什么板（列出来问，不问不许开工）：无（用户已授权开发）；仅在出现与批准规则冲突的必要取舍时集中提问。
- 环境事实（2026-10-09 实测）：JDK 17.0.20.1（`$HOME/android-toolchain/jdk-17.0.20.1+1/Contents/Home`）、SDK platforms 34/35/36、build-tools 34/35/36；样机构建配置 Gradle 8.14.3 / AGP 8.13.0 / Kotlin 2.1.20 / Media3 1.11.1 / compileSdk 36 / minSdk 31 / targetSdk 36。
- 真机：目标机 `IN9LZTAYV4UGU4JF`＝Redmi Note 11T Pro（22041216UC），已装技术样机 `com.orca.audiopoc`；另有 `indq5xfi6hovay4d`＝22101316C（装了 Orca 自己的 App，非本项目）。
- **治理修复任务《P045 worktree 错位修复＋铺包防呆》收单并排队**（2026-10-07；任务书自定"本阶段开发完成后才执行"）。现状已核：铺包全落在 `1.Active/main`（worktree/分支 wanghoufan/main），主仓 `045-ing-凯格尔训练`（main）仅空壳＋未跟踪 V1.1 草稿；两 worktree 同 commit 2914c25。
  - **已做（可回退保护）**：把 `1.Active/main` 全部未提交内容备份至 `045-ing-凯格尔训练/temp/worktree-backup-2026-10-07/`（137MB）。
  - **待用户明确指令（红线）**：A2 commit（分支 wanghoufan/main）→ A3 merge 入 main → A4 `git worktree remove 1.Active/main`（不可逆）→ A5 重建 `.worktree/main`；禁 push/merge 远端写入除非另令。
  - **Task B（母版防呆）**：改母版规范（项目根 `.git` 须为目录；worktree 路径须在项目内；铺包目标=项目根、禁相对路径 worktree add；铺包后 ls 校验）；母版红线流程：先出融合章节草案→用户审→写入→`_sync-packages.py` 同步两包→补概览→`check-sync.sh` SYNC-OK→经验一句话。
- 约束（用户令）：不删技术验证；不重开项目；不重跑 Skill benchmark；不改 ORCA 母版；不 git push。
- 收尾记一笔：待收尾派 neat-freak。
- 【jcp】小清理（2026-10-10，签收后阶段清理，用户授权自决）：删已合并本地分支 `wanghoufan/main`（6878e64，已并入 main）；`temp/ProductPlan-V1.1-备份-1725.md`、根 `AGENTS.md.旧版-2026-10-08` 移入 `~/.Trash`（均有 git/现役权威版本）；`.gitignore` 补 `.arts/`＋`local.properties`。保留：`temp/worktree-backup-2026-10-07/`（137MB，A2–A5 治理修复的依赖备份）＋`temp/接续提示词-P045-*.md`（该任务待令）。

## 恢复读盘（全体系唯一顺序，别乱）

1. AGENTS；2. 角色卡；3. 根 `USER_MODEL_OVERRIDE.md`；4. 本 HANDOFF；5. 根 `经验一句话.md`；6. 任务目标放最后。
冲突才扩大读。

- 2026-10-13｜Change C：剂量与进阶体系受控重开；PROJECT_PHASE=PLAN_REOPEN_REQUIRED；planner已交 docs/pm/PRODUCT_PLAN_V2.0.md 与 DOSE-EVIDENCE-V2.0.md（82/100、P0=1、blocking P1=1，待独立研究/专业复核后 Human Approval，PLAN_GATE=IN_PROGRESS）；DEV_BASELINE 冻结待重签，旧 DEV-V1 ACCEPTED 不变，禁据此开240高量或开发发布。
- 【Change C 重开｜2026-10-13 用户令】用户实测判定「L6 每日 40 次太少、新手都能达到」，令 planner 按研究最高标准重设计剂量与进阶体系。流程：planner（codex/gpt-6.1-sol）产出 `docs/pm/PRODUCT_PLAN_V2.0.md`＋`docs/pm/DOSE-EVIDENCE-V2.0.md`（证据 17 源；六级日量 30/45/60/90/120/240，L6 为耐力120＋快速120 合计、满量 6 会话；自评 82/100）；product-reviewer（codebuddy/glm-5.3-flash）串行独立复核 `docs/review/RESEARCH_REVIEW-V2.0-R1.md`＝`ACCEPTED_WITH_CONDITIONS`、建议 82/100。
- 【用户修正令 B2（覆盖 V2.0 的量权限条款）】用户明确：取消「>45/日需专业书面审查」「>120 及 L6 需个体专业指导」「新用户 C≤45」「阶段日额/许可上限冻结」「分期解锁」等一切外部许可门槛；选 L6 当日即可按 240 满量执行，不做爬坡。保留：单次保持 ≤10 秒、同类组 ≤10 次、单会话 ≤40 次、日额 ≤240、组间 30 秒完全放松、疲劳/疼痛即停与半量降级、合规话术、中英双语与主题三态。
- 【新 DEV_BASELINE】`PRODUCT_PLAN_V2.0 + 用户修正令 B2`（2026-10-13 用户令，取代 `PRODUCT_PLAN_V1.1 + PRODUCT_RULES_APPROVED`）。`PROJECT_PHASE=DEVELOP`（Change C 受控重开完成、用户放行开发）。当前 Task：TASK-020（builder 派工中，codebuddy/deepseek-v4.1-flash，prompt 落 `temp/dispatch-builder-dose-v2.md`，日志 `temp/builder-dose.log`）；后续：code-reviewer → qa → supervisor 复检；真机目标 Note 11T Pro（已固定）。旧 40 上限不追溯改写历史记录。
- TASK-020（builder，DEV-V2 剂量体系实现）**已交付待复核**（2026-10-13）：按 `PRODUCT_PLAN_V2.0 + 修正令 B2` 落地新六级矩阵（L1–L6 日量 30/45/60/90/120/240，H 含收紧起始、每 10 次拆组、组间/跨类 30 秒可延长不可缩短、单会话 ≤40/日 ≤240）＋确定性额度换算（`B=min(D,240)`、`E_B/F_B`、半量按类折半、按时段分配、启动前按双维实际已用取小、中止不返还）＋会话次数 1/2/3/4/6 与提醒 1/2/3 分开。改动面：`domain/Levels.kt`、`domain/Plan.kt`、`domain/History.kt`、`data/AppRepository.kt`、`ui/AppViewModel.kt`、`ui/KegelApp.kt`、`ui/TrainingScreens.kt`、`audio/TrainingService.kt`、`audio/TrainingState.kt`、`res/values*/strings.xml`、`test/.../DomainTest.kt`。验证：`./gradlew assembleDebug testDebugUnitTest` **BUILD SUCCESSFUL**（36 单测全绿，`tests=36 failures=0 errors=0`）。AC-54..61 及重写 AC 已在本矩阵建行、状态 `OPEN`（真机/UI/听感待 qa）。未 commit/push。
- TASK-020 返工（builder，2026-10-13）：修 `docs/review/CODE_REVIEW-V2.0-DEV2.md` 三条 P1＋可行 P2——P1-1 摸底实际开始的收缩计入当日额度（`AssessmentScreen`＋`AppViewModel.addAssessmentUsage`＋`AppState.usedE`；快速滑块自报不扣；余量不足提示次日）；P1-2 会期间隔 ≥2 小时（`SessionSpacing`＋`PlanResult.IntervalBlocked`＋首页可见说明/延后出口）；P1-3 疲劳即停（`SafetyReason.FATIGUE`＋双语＋安全出口项）与疲劳后次日整日半量（`pendingHalfDate`）；P2 跨午夜按发生日扣额（`SessionRecord.startedByDay`）、删死代码 `startLevelPreview`、`doseVersion` 旧=1/V2=2 可追溯。改动面追加 `ui/AssessmentScreen.kt`。验证：`./gradlew assembleDebug testDebugUnitTest` **BUILD SUCCESSFUL**（43 单测全绿，`tests=43 failures=0 errors=0`，新增 7 例独立硬编码断言）。待 code-reviewer 复检 → qa 真机。未 commit/push。

- 2026-10-13｜planner 参数勘误：已交 `docs/pm/PRODUCT_PLAN_V2.1.md`（待批准，批准后取代V2.0剂量分配；V2.0其余条款仍有效，B2取消量权限门槛继续有效）；耐力回V1.4每次5/6/8/8/10/10、快速日10/20/30/50/80/120、快速组20/日硬限240、单会话合计≤40；L6 N6每次10+20、4:56/日29:36，非13–15/60–90分钟；证据总量口径已勘误，60种规划自查通过；不改业务/治理表、DEV_BASELINE或PROJECT_PHASE，不commit/push。

- 2026-10-13｜【V2.2 剂量矩阵生效｜TASK-022】用户直接批准「三张表」口径并**跳过 planner**（V2.0/V2.1 剂量分配作废；B2 取消量权限门槛继续有效）。**新口径**：先定每场次数，日量＝每场次数×会话数（整除、每场全天一致，无 17/17/16 余数分摊）。每场＝**1 个耐力组（≤10）+ 1 个快速回合（≤20）**。
  - 耐力：场数 3/3/4/4/5/6、每场 5/6/7/8/9/10、日总 **15/18/28/32/45/60**。
  - 快速：回合数 3/3/4/4/5/6、每回合 5/8/10/12/16/20、日总 **15/24/40/48/80/120**。
  - 每日合计：**30/42/68/80/125/180**；单次时长 106/120/136/168/228/296 秒；日上限 240 仅保护。
  - 改动面：`domain/Levels.kt`（V2.2 矩阵＋`endurancePerSession/fastPerSession`＋`FAST_GROUP_MAX=20`＋`sessionOptions` 加 5）、`domain/Plan.kt`（快速按 20 拆回合、`Quota.daily` 改为每场×N）、`ui/KegelApp.kt`（各级标准页重做为三张表）、`res/values*/strings.xml`（中英新表头/说明）、`test/.../DomainTest.kt`（断言更新＋新增整除/一致性用例）。
  - 验证：`./gradlew assembleDebug testDebugUnitTest` **BUILD SUCCESSFUL**，单测 **45 全绿**（tests=45 failures=0）；真机 `IN9LZTAYV4UGU4JF`（Redmi Note 11T Pro）`install -r` → **Success**，各级标准页三张表逐行核对与批准表一致（证据 `docs/qa/evidence-P045/50-52-v2.2-*.png`）。**未 commit/push**。
  - 待办（治理）：V2.2 的 code-reviewer/qa/supervisor 正式链**未单独派**（用户直接令、本窗口「唯一实现者直做」，记 `executed_by=task-manager`，`chain_status=OPEN`）；如需正式复核可补派。

- 2026-10-13｜【TASK-023｜用户两令：删 2 小时间隔闸 ＋ 新增「历史」tab】
  - ①**删除首页 2 小时会话间隔安全闸**（用户令 A1）：`PlanResult.IntervalBlocked`、`SessionSpacing`、`AppRepository.canStartNextSession/nextSessionAllowedAtMs/lastSessionEndMs`、首页 `IntervalBlocked` 分支、文案 `home_interval_blocked/wait`、对应测试全清。**首页永远显示「开始今日训练」**，恢复用户早前「开始始终可点、安全锁只处理真实异常」规则。
  - ②**新增底部「历史」tab**（今日/能力/历史/设置 四页）：`HistoryInsights`（`summary` 总计、`daily` 近 N 天按日聚合，纯函数可单测）＋ `HistoryScreen`：总计卡（总训练次数/训练天数/总时长/总次数/耐力总/快速总/累计保持）＋ 两张柱状图（每日训练时长、每日总次数，近 7 天）＋ 近 14 天每日明细表；中英文案齐备；无新增第三方依赖（图表用 Compose 布局自绘）。
  - 验证：`assembleDebug testDebugUnitTest` **BUILD SUCCESSFUL**，单测 **44 全绿**（移除 2 间隔用例、新增 1 历史聚合用例）；真机 `IN9LZTAYV4UGU4JF` `install -r` → **Success**，首页无拦截、历史页统计与图表逐项核对通过（证据 `docs/qa/evidence-P045/53-55-v2.2-*.png`）。**未 commit/push**。
  - 待办（治理）：同 TASK-022，正式 code-reviewer/qa 链未单独派（`executed_by=task-manager`，`chain_status=OPEN`）。
  - 2026-10-13 补（用户验收反馈）：①「每日明细」改为两段式行（左日期 ←→ 右「场/时长 m:ss/次数」），去等宽字体、时长改 `9:23` 紧凑格式，解决列挤在一起；②底部 4 个 tab 改用 Material 图标（今日=Home、能力=Star、历史=DateRange、设置=Settings，material-icons-core 随 material3 自带，无新依赖）。44 单测绿，真机 install -r Success 并核对（证据 `56/57-v2.2-*.png`）。
  - 2026-10-13 再补（用户验收反馈二轮）：①**首页移除「今日已用 / 今日剩余」两行**（用户令：练一遍即达标，不要额度显示）；②**能力页移除「最近训练」列表**（与历史页重复，保留等级变化）；③「每日明细」改为真正的**四列（日期｜场次｜时长｜次数）等宽居中**，列间清晰不再挤。44 单测绿，真机 install -r Success 并逐项核对（证据 `58-60-v2.2-*.png`）。
