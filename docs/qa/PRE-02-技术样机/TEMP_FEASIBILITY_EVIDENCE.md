# TEMP_FEASIBILITY_EVIDENCE ｜ 技术可行性现场（非正式进度）

> 用户 2026-10-07 纠偏令：本目录与 `prototype/audio-poc/` 是 **Android 音频技术 spike** 的现场，原样保留，**降级为 `TEMP_FEASIBILITY_EVIDENCE`**。

- 它验证了什么：目标真机（Redmi Note 11T Pro / Android 14 / API 34）上，原生 Kotlin + Media3(MediaSessionService) 能否满足——
  - 锁屏/后台持续播放（S1）；耳机拔插（S2）；来电/音频焦点抢占与返回（S3）；运行通知/锁屏播控暂停恢复（S4）；无声/锁屏暂停与解锁恢复（S5）。
  - 证据：本目录的录屏（S1/S3/S4 .mp4）、截图、dumpsys media_session/audio 日志、App 事件日志。
- **不代表**：Prototype、Design Freeze、SDD PLAN/TASK、requirements-traceability、Phase2 开发完成；**不计入正式项目进度**。
- 用途：后续进入 SDD 技术选型阶段时，作为复选矩阵输入，决定 **KEEP / REUSE / REMOVE**。当前不删除、不继续扩成产品。
- 产品需求真源仍为用户提供的 Product Plan（`2026-10-07 - Mac Mini - ChatGPT - 男性盆底肌语音训练器Product Plan-计划 - V1.1.md`）；不得用技术样机反向定义产品。
