# 盆底肌训练（Pelvic Trainer）

[English](./README.en.md)

一款专注男性盆底肌的**语音训练 App**：戴上耳机跟着语音做「收紧—保持—放松」，不用盯屏幕、不用自己数数；从 1–6 级逐级进阶，训练自动记录。

| 今日首页 | 能力记录 | 设置 |
|---|---|---|
| ![今日页：一键开始今日训练](./docs/screenshots/home-zh-light.png) | ![能力页：耐力与快速控制统计](./docs/screenshots/ability-zh-light.png) | ![设置页：主题与语言](./docs/screenshots/settings-zh-light.png) |

## 它解决什么问题

凯格尔（盆底肌）训练最难的不是动作，而是**坚持**：传统 App 要你盯着屏幕看倒计时、心里默数次数，练起来既麻烦也容易分心。这款 App 把整个训练交给语音——耳机里报「收紧」「保持 3、2、1」「放松」，你只管跟着做；坐着站着都行，通勤、办公间隙都能练。

## 谁适合用

- 想开始或已经在做凯格尔训练、但不想被屏幕绑住的男性用户
- 希望有明确等级和目标节奏（而不是自己瞎定秒数次数）的训练者
- 在意数据隐私、希望记录只留在自己手机上的用户

## 核心功能

- **一键开始今日训练**：按当前等级自动生成「耐力 + 快速」两段训练脚本，全程语音引导（中文 / English 双语语音）
- **可选的能力摸底**：5 个摸底项（动作控制、耐力保持、耐力次数、快速次数、放松质量）全部内置计时/倒计时＋语音提示，不需要手动计数；测完给出 L1–L6 **训练等级建议**
- **六级递进标准**：每一级的保持秒数、休息、次数都有明确单值标准，可在「各级标准」页随时查看；升级由多个维度共同达标决定，不看屏幕也知道进度
- **训练可控**：暂停、恢复、提前结束、安全出口（疼痛等情况下立即停止）；低调模式全程无声、仅字幕与倒计时
- **额度与频率**：每日提醒 1/2/3 次可选，选 3 次自动降量；每日总收缩封顶 40 次，支持「今日半量」
- **能力页**：耐力 / 快速分开统计，训练天数、次数、等级变化历史一目了然
- **主题三态 + 中英双语**：浅色 / 深色 / 跟随系统；简体中文 / English，其他系统语言自动回退中文；切换不丢训练状态

## 快速开始（从源码构建）

环境：JDK 17、Android SDK（compileSdk 36 / minSdk 31）、随仓 Gradle Wrapper。

```bash
export JAVA_HOME=$HOME/android-toolchain/jdk-17.0.20.1+1/Contents/Home
export ANDROID_HOME=$HOME/android-toolchain/sdk
./gradlew assembleDebug          # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 领域逻辑单元测试（21 个）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

语音提示资产可离线重新生成（macOS，无需联网）：`bash tools/make_cues.sh`。

## 健康边界

- 这是**训练辅助与自我记录工具**：不做医疗诊断，不承诺治疗效果（包括 ED/PE），App 内摸底不等于临床肌力评估。
- 出现疼痛、持续异常紧张或明显排尿排便问题时，请使用「安全出口」停止训练并咨询医生或盆底物理治疗专业人员。
- 所有数据只保存在本机应用私有目录，无联网、无账号。

## 项目结构

| 目录 | 内容 |
|---|---|
| `app/` | Android 应用（Kotlin 2.1.20 + Jetpack Compose + Media3；包名 `com.orca.kegel`） |
| `docs/pm/` | 产品计划书与需求真源（Product Plan V1.1） |
| `docs/design/` | 设计契约：交互规范、冻结说明与可点击原型（`docs/design/prototype/`） |
| `docs/qa/` | 产品验收追踪矩阵（34 条 AC）与逐条证据截图（`docs/qa/evidence-P045/`） |
| `docs/roles/`、`docs/handoff/` | 开发流程治理文档（ORCA 角色卡与交接记录） |

## 当前版本

`v1.0`（versionCode 1）· Android 12 及以上 · 简体中文 / English。
