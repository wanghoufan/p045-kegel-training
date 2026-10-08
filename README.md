# ORCA 治理模板分发版｜导航（先读我）

[English](./README.en.md)

> 模板名冻结，版本真相以本仓库 Git 提交历史为准（版本标记文件仅为指针）。

开工读盘顺序（全体系唯一）：`AGENTS.md` → `docs/roles/`（本次角色卡）→ 根 `USER_MODEL_OVERRIDE.md`（模型表，有就用它）→ `docs/handoff/`（交接现状）→ 根 `经验一句话.md` → 任务目标放最后。

## 一图看懂（体系全景）

**两阶段治理总流程**（口令推进 · Human Gate 不可自动跨越 · Change C 闭环）：

![ORCA 两阶段治理总流程](./docs/assets/orca-two-phase-flow.png)

**角色阵容与 Phase 2 派工主链**（9+1+1 · 自动探测派工口 · 账本与同步三件套）：

![ORCA 角色阵容与派工主链](./docs/assets/orca-roles-dispatch-chain.png)

## 怎么用：你的操作只有两件事（新建文件夹 ＋ 说一句话）

体系不绑定 Orca，也不绑定任何客户端：**Orca／Trae／Qoder／Codex／CodeArts Agent／opencode／Claude Code** 随便切，编排者每轮开工跑 `scripts/detect-client.sh` 自动认当前客户端并选派工口（有原生子代理就在该客户端窗口内直派；没有就走通道 CLI），**不需要你填任何配置、不需要你指派角色**。

| 场景 | 你要做的 | 说哪一句 |
|---|---|---|
| **新项目·大项目**（多阶段要等你点头／要产品验收留痕／跨周或会交接／要发布上线留回执／多角色并行，命中任一） | ①新建一个空文件夹 ②在里面打开任一客户端 | 见下方【新项目大项目·一句话】（它会自己铺包、建账本、认客户端、派活） |
| **新项目·小项目**（单文件小修／单日单任务／一次性脚本／查资料） | 新建文件夹 ＋ 打开客户端 | `直接干活，别铺包。项目是 XXX，要做 XXX。` |
| **老项目**（本机 `1.Active/` 里已铺好的） | 打开该项目 ＋ 打开客户端 | 直接说事即可：`XXX 项目，第一阶段，计划`（规则、脚本、账本都已就位，**不要重铺包**） |
| **单客户端环境**（只有 shell、没有子代理） | 同上 | 完全一样；探测返回 `channel_cli`，角色走 `opencode run`／`codebuddy -y -p`／`codex exec` 直调，规矩不减，只是少了并行与真 resume |

### 新项目大项目·一句话（复制即可）

```text
按 /Users/zzymima0000/Developer/coding/4.Templates（PC）/2026-09-09 丨 MAC 丨 ORCA V2.1 治理模板 丨 分发版-2026-09-11/新项目模板包/README.md 和它的归位表，把 ORCA 治理模板铺进这个项目根目录（USER_MODEL_OVERRIDE.md 建软链指母版，别拷实文件），铺完你就是本项目编排者。项目是 XXX，第一阶段，计划。
```

之后每一单你只说**业务目标 ＋ 口令**（下面三个口令），铺包/派工/落账/验收全由编排者自己走完。

**别做的事**：不要把 `新项目模板包/` 整个文件夹丢进项目根（会多一层目录、路径全错）；不要拷母版 Git 历史或别的项目的 HANDOFF；老项目不要重铺（规则升级由母版 `scripts/sync-old-projects.sh` 自动铺开）。

## 用户只需记住三个口令

- `第一阶段，计划`：进 Phase1（PLAN），Planner＋Research Reviewer 出 PRODUCT_PLAN，≥90 且模板 Gate 全条件满足（P0=0＋blocking P1=0＋事实/假设验证）才找你。
- `第二阶段，开发`：Human Gate 批准后进 Phase2（DEVELOP，锁定 DEV_BASELINE），默认主链开发（模型以override表为准）。
- `变更请求：……`：开发中反馈统一入口，TM 按 A（小改留 DEVELOP）/ B（局部功能改留 DEVELOP 不召 Sol）/ C（产品架构变 Controlled Reopen）分类。

## 模型口径（一句话）

模型／通道／调用方式**一律以根 `USER_MODEL_OVERRIDE.md` 表为准**（唯一口径，改表必真调，精确 ID 照抄执行）；本 README 不复述模型 ID，避免与表漂移。换人用户直接改母版真源表；表内无备用列。分工表软链制：各项目根表均为软链指母版真源，改母版即全项目同步（禁拷实文件；跨机器断链时拷实文件并记 HANDOFF）。

## 根目录

**入口与规则**
- `AGENTS.md`：全员规则（一页，全体系唯一真源之一）
- `README.md`：本导航（先读我）；`README.en.md`：英文导航
- `ORCA治理体系说明.md`：**对外概览**（体系更新三件套第 2 步必须同步它）
- `USER_MODEL_OVERRIDE.md`：模型表（角色/模型/执行通道/调用方式，**唯一口径**，改表即生效，精确ID照抄执行）
- `GOVERNANCE_VERSION`：版本指针文件（内容：以Git历史为准）
- `经验一句话.md`：收工一句经验（只追加）
- `agent.md`：接续开工提示词快照（**临时材料，随交接更新，不受 check-sync 门禁**）

**脚本（`scripts/`）**
- `detect-client.sh`：自动认当前客户端并选派工口（`window_subagent`／`channel_cli`）；每轮开工先跑它
- `check-channel-preflight.sh`：分工表在用模型 ↔ 通道目录对账，须 `CHANNEL-OK` 才可派工
- `weekly-channel-check.sh` ＋ `_inject-agents-block.py`、`sync-old-projects.sh`、`migration-status.sh`、`_sync-packages.py`（母版→两包同步唯一入口，**禁手写 sed 复制**）
- `model/check-ledger.mjs`：两本账本校验（老项目迁移登记检查用它，须 `LEDGER-OK`）
- `decision/orca-decide.mjs`：决策侧车；`orchestration/`：L3 watchdog（仅外部/终端编排时部署）
- `check-sync.sh`：母版↔两包一致性门禁（归一化裸名后逐字节比对），须 `SYNC-OK`

**包**
- `新项目模板包/`（新项目初始化）、`老项目迁移模板包/`（老项目迁移，入口【迁移整理】提示词）。两包内容由 `scripts/_sync-packages.py` 从母版生成，**不要手改包内文件**。

## docs/ 地图

- `roles/`：11张角色卡（只看本次派的角色）
- `prompts/`：编排者/外部开发者/迁移整理三份提示词（迁移整理含自举取包＋冲突处理，可当老项目唯一入口）＋《Orca 通用编排者持续推进协议》（防停摆三层监督收编版；其动态角色论与十卡制冲突，不采用）＋《Orca 编排治理监督者提示词》（总监督wake-only收编版，平时只喊编排者）
- `history/`：重塑说明、更新说明、ORCA 模型与双阶段治理整改方案（历史，看现行先看根）
- `templates/`：归位表模板
- `pm/` `qa/` `review/`：计划/测试/评审落盘（各照 template）
- `handoff/`：交接（含模板）；`model/`：模型账本（TASK 首个真实任务前、DISPATCH 首个真实派工前删示例行）＋ TM 资格测试（`TASK-MANAGER-QUALIFICATION.md` 规范/报告、`TASK-MANAGER-QUALIFICATION-EVENTS.jsonl` 事件，评分 `scripts/model/tm-qualification.mjs`）
- `sop/`：基础设施规范（docker.md、supabase.md、sqlite.md、android.md、android-machine-profile.md、webqa.md、decision-router.md，去版本号引用），新项目自建
- `scripts/decision/`：Decision Sidecar orca-decide（用法见其 README 与 docs/sop/decision-router.md）

## 已删除（用户令，结论均已落实）

- 历史治理审查报告6份（09-09→09-11）：P1/P2结论已全部修完验过，删前状态见 git 历史；现行结论以包内文件为准
- `docs/model/模型分工工作量排名-2026-09-13-参考.md`（2026-09-15 删，快照过期，见 git 历史）
- 根散文件已归位：三提示词→`prompts/`、两说明→`history/`、归位表→`templates/`

## 改名对照（去版本号一次改名，老链接按此找新位置）

- `docs/history/2.0-重塑说明.md` → `docs/history/重塑说明.md`
- `docs/history/2.1-更新说明.md` → `docs/history/更新说明.md`
- `docs/history/ORCA-V2.1-治理审查报告-*` → `docs/history/ORCA-治理审查报告-*`（实物已删，见 git 历史）
- `docs/history/*ORCA V2.1模型与双阶段治理-整改方案 丨 V1.0.md` → `docs/history/*ORCA模型与双阶段治理-整改方案.md`
- `docs/prompts/*持续推进协议 丨 V1.1.md` → `Orca 通用编排者持续推进协议.md`
- `docs/sop/*交接上下文 丨 V1.0.md` → 去掉末尾 ` 丨 V1.0`（该文件已删，见 git 历史）
- 口令 `【迁移整理｜2.1】` → `【迁移整理】`
- 口令 `【编排者｜2.1开工】` → `【编排者｜开工】`
- 口令 `【外部施工｜2.1】` → `【外部施工】`

## 可直接使用的模板包

- `新项目模板包/`：新项目初始化所需的完整文件包。
- `老项目迁移模板包/`：老项目迁移所需的完整文件包；入口是 `【迁移整理】` 提示词（含自举取包：它每次都自己去本机治理仓库取包，无需你手动拷贝，也不会因项目已有治理文件而跳过）。
