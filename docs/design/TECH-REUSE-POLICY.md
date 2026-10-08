# 技术复用政策 ｜ P045（用户 2026-10-07 令，全流程生效）

> 适用于：Research、UX、Prototype、Reuse Audit、SDD、TASK。**不因本政策提前进入正式开发。**

## 决策类型（统一枚举）

`REUSE / ADAPT / REVIEW_FOR_REUSE / BUILD`

## 优先级（Market/Official Reuse First → Own Code Reviewed Second → Custom Build Last）

1. **REUSE**：官方成熟框架 / 官方 SDK 优先。
2. **REUSE**：市面成熟、持续维护、社区验证充分的主流库。
3. **REUSE**：License 清晰、质量可靠的成熟开源实现。
4. **REVIEW_FOR_REUSE**：用户自己的旧项目代码——**必须先审查质量**，通过后才可复用（不得直接搬用）。
5. **BUILD**：以上都不适合时，才允许自建。

（`ADAPT`＝在 1–3 类之上做受控改造；需说明改造点与理由。）

## 关键节点

- 技术 spike 仍只是 **feasibility evidence**（`TEMP_FEASIBILITY_EVIDENCE`），不因本政策升级为正式开发。
- **正式 Reuse Audit 放在 Prototype Approval 之后、SDD 之前。**
- **SDD 必须带技术复用矩阵**：对每个主要 capability 说明为什么是 REUSE / ADAPT / REVIEW_FOR_REUSE / BUILD，并给出所选库/组件、版本、License、社区与维护证据。

## 记录

- 2026-10-07：用户令追加，登记于此并写入 HANDOFF。
