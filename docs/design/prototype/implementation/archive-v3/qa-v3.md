# PROTO V3｜Human 2 第二轮整改 QA

EVIDENCE_MODE: NONE
BROWSER_EXECUTED: NO
CONSOLE_INSPECTED: NO
RUNTIME_EVIDENCE_REFS: ["runtime-evidence/runtime-attempt.json"]
PROTOTYPE_RESULT: PENDING_RUNTIME_VALIDATION
PROTOTYPE_GATE: PENDING
INTERACTIVE_PROTOTYPE: PENDING

## 五槽与正式绑定

| 槽 | 交付 |
|---|---|
| HTML | [prototype.html](prototype.html)，自包含移动端可点击模拟器 |
| 资产 | manifest `EMPTY_EXPLICIT`，无远程字体/图/脚本/音频文件；系统语音复用浏览器 |
| manifest | [PROTOTYPE_MANIFEST.json](PROTOTYPE_MANIFEST.json)，UX-V1.3的17屏/72交互/62状态精确一致，当前prototype/UX/Direction三hash |
| 截图 | [PROTOTYPE_SCREENSHOT_INDEX.md](PROTOTYPE_SCREENSHOT_INDEX.md)，62正式状态逐条NOT_CAPTURED |
| QA | 本文与当前hash绑定的实际运行预检记录 |

## 实际验证

- [ux-v3-validation.json](ux-v3-validation.json)：需求源/用户方向/完整交互字段/十类状态/可达图/需求链校核；原14FR/26AC有效、未知ID=0，UX gate=PASS。
- [static-rule-checks.json](static-rule-checks.json)：86项批准矩阵/阈值/频率/日40额度/日期/统计/EHS/内联语法检查通过；V3摸底不封顶推荐、不设置旧等级锁。
- [static-v3-path-checks.json](static-v3-path-checks.json)：47项实际handler函数检查，通过无级→选L1→开始、有级一次开始、今日/能力标准、独立等级持久化及默认建议、真实配置损坏手动恢复、活动改级快照独立、紧松与完整动作窗口、日40及真实异常中止。
- [static-integrity.json](static-integrity.json)：正式ID三组集合与HTML元数据完全一致，所有interaction有可执行handler，方向证据/版本及当前hash一致。Product Plan、Direction、Research、批准补充文件与技术复用政策保持本轮输入字节。
- Node测试用模拟对象、存储与假语音回调。**不是真实浏览器点击、实际声音、console检查或截图，不支持runtime PASS。**

## R1–R5 落地

- R1：说明重复能力/不是再测极限；一次按刚测保持秒数收紧并保持→完全放松；两次之间完全放松＋休息约5秒；完成一次点一次X/10，可结束本项。
- R2：今日/能力可打开各级标准，六级单值矩阵；单次、默认每日2次与当前1/2/3频率估算，三次降量及非医学肌力/非诊断明确。
- R3：开始按钮正常始终可点击，取消旧跨会话锁；配置损坏可手动使用内置批准矩阵选级继续。日40、明确选择休息日、已有会话等真实边界仍反馈，不用手动路径绕过。
- R4：耐力短提示“准备/收紧/放松/休息/结束”；快速动作只“紧/松”，快速专项准备/结束仅界面提示；提示播完后才计完整动作窗口，异常明确中断，不抢播。
- R5：随时改当前选择，摸底与选择独立显示/持久化；上次结果作下次可改默认建议；进行中/暂停可选下次级别，当前会话快照不变。真实不适仅停止本次，不产生永久选级锁。

## 运行尝试及实际限制

本轮BrowserOS run预检被审批策略拒绝：`MCP tool call requires approval, but approval policy is never`。未获得页面，也未实际检查console或截图；没有绕过审批或改用其他QA基础设施。

[runtime-attempt.json](runtime-evidence/runtime-attempt.json)绑定当前三hash，是未成功预检记录：browser=false、console inspected=false、blocking console errors=null、page errors=null，实际交互/状态/截图为空。62状态均NOT_CAPTURED。

用户报告在桌面Chrome实点V2且声音正常，记为**用户报告的旧版体验证据**；不伪造为Agent执行，不支持V3放行。旧证据归档archive-v1/archive-v2，hash或样例截图不得用于本版PASS。V3 Human2批准仍为空。

## REQUIRED_RERUN

1. 在获准桌面浏览器打开当前HTML；清空示例：无等级选L1→开始；载入L3→今日一次开始；打开今日/能力各级标准并返回。记录实际INT/ST。
2. 自选跨级、进行中改下次级别与暂停改级；试摸底较低/未达L1/取消，当前选择不得锁死；关闭重开验证两条信息、最新结果建议可改。
3. 摸底耐力次数读说明，按测得秒数保持与完全放松＋约5秒休息；试X/10、最近记录、10次封顶、结束本项和SKIP。
4. 在系统语音真实秒数下完整今日及快速专项，实际听“紧/松”不重叠且动作窗口足够；耐力短提示。检查暂停/恢复、结束确认/取消、中断返回、真实异常停止、安全中止不可复活。
5. 模拟配置损坏→手动L1→开始；频率3降量/日40/休息选择/14日提示/EHS及写失败重试；查看历史实际量与旧等级快照。
6. 检查console/pageerror；逐条执行manifest要求的72交互/62状态（契约未标critical子集）。状态样例不能替代真实路径。拍可读截图，绑定SCR/ST、相对路径和图片hash，更新索引。
7. 新runtime JSON绑定当前三hash；实际browser=true、console inspected=true、真实错误数、INT/ST和截图记录。全部验证后才同步QA/manifest/证据为PASS；否则如实PENDING/BLOCKED。

## 复用与批准边界

REUSE原生HTML/CSS/DOM/Web Storage及系统语音；ADAPT批准规则、独立持久化及语音完成回调时序。旧技术样机NOT_REUSED，后续仍REVIEW_FOR_REUSE；未BUILD底层引擎。

示例专用存储可清除，不影响真实产品数据。来电/音频焦点/锁屏为明确模拟，不证明Android后台；20倍快进/仅模拟提示不证明Audio First。Human2反馈是整改授权，未批准V3、未跨Design Freeze或正式开发授权。
