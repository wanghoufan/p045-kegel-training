# PROTO V4｜起练零前置 QA

EVIDENCE_MODE: NONE
BROWSER_EXECUTED: NO
CONSOLE_INSPECTED: NO
RUNTIME_EVIDENCE_REFS: ["runtime-evidence/runtime-attempt.json"]
PROTOTYPE_RESULT: PENDING_RUNTIME_VALIDATION
PROTOTYPE_GATE: PENDING
INTERACTIVE_PROTOTYPE: PENDING

## 五槽

| 槽 | 当前交付 |
|---|---|
| HTML | [prototype.html](prototype.html)，开始同步首个收紧/紧，无准备或试听确认页 |
| 资产 | manifest EMPTY_EXPLICIT，自包含原生HTML/CSS/JS与平台语音 |
| manifest | [PROTOTYPE_MANIFEST.json](PROTOTYPE_MANIFEST.json)，UX-V1.4的16屏/68交互/58状态精确一致及三当前hash |
| 截图 | [PROTOTYPE_SCREENSHOT_INDEX.md](PROTOTYPE_SCREENSHOT_INDEX.md)，58条NOT_CAPTURED |
| QA | 本文与当前绑定的实际运行预检JSON |

## 实际静态校核

- [ux-v4-validation.json](ux-v4-validation.json)：原14FR/26AC有效、未知编号=0、结构/状态适用/屏可达/需求链通过，UX gate=PASS。
- [static-rule-checks.json](static-rule-checks.json)：86项批准剂量/额度/频率/统计/EHS及内联语法校核通过；时长仅删除准备10秒。
- [static-v4-start-checks.json](static-v4-start-checks.json)：27项实际handler函数检查通过：同一点击首相位/首提示、无setTimeout或语音回调等待、快速紧、无声音/播放失败继续、六级时长、首周期去重、充分放松与手动继续。
- [static-integrity.json](static-integrity.json)：正式HTML属性ID三集合、handler全集、方向用户证据与当前版本/hash精确匹配。SPEC、DIRECTION、RESEARCH、批准规则源、Product Plan及技术复用政策保持本轮输入hash。
- Node模拟对象及假语音回调不是真实浏览器DOM/声音/console/截图，不能作为runtime PASS。

## 单项受控变化

用户本轮V4覆盖原准备10秒与音频资源前置；点击立即首个E/F相位，耳机自选，语音按节奏提交，不可用显示字幕但训练继续。没有准备倒数、250毫秒预检、试听或戴耳机确认。训练内充分放松、组间/结束10秒、单会话、日40及安全停止保持。

退役对象与原编号保留策略见[变更说明](PROTOTYPE_V4-变更说明.md)。ST-21现在是训练中可选音频不可用反馈；不作为前置阻断。原SPEC及源文件不修改，UX记录本轮用户明确授权。历史会话原时长快照保持；旧PREP示例检查点迁移后仍需主动恢复。

## 运行尝试及实际限制

本轮BrowserOS run预检被审批策略拒绝：`MCP tool call requires approval, but approval policy is never`。未获得真实页面，未检查console/pageerror，未截图。没有绕过审批或更换QA基础设施。

[runtime-attempt.json](runtime-evidence/runtime-attempt.json)是当前hash绑定的未成功预检记录：browser=false、console inspected=false、blocking errors=null、page errors=null；实际执行INT、观察ST及截图为空。用户实点V3属于旧版体验报告，不是V4 Agent运行/批准证据。旧证据归档archive-v3及早期目录，不支持V4 PASS。

## REQUIRED_RERUN

1. 获准桌面浏览器打开当前HTML，载入已有等级；点“开始今日训练”，同一次点击立刻看到首个收紧及矩阵秒数、听到当前短提示；无准备/预检/试听页。
2. 清空示例，无等级选L1后开始；快速专项同样即首个“紧”，不等待语音回调。去掉声音能力/模拟播放失败后相位/时钟仍继续，有字幕反馈；不需戴耳机确认。
3. 实际检查console/pageerror，记录INT-12/13/63/70、ST-11/13/21及真实截图。旧状态样例不能支持实际路径；完整放行仍按manifest正式交互/状态要求复核。
4. 当前runtime证据JSON绑定prototype/UX/Direction三hash，真实INT/ST、每张SCR/ST/路径/图片hash；更新截图索引。全部要求通过才同步QA/manifest/runtime PASS；未知错误数不得写0。

## 复用与批准边界

REUSE原生HTML/CSS/DOM/Web Storage/系统语音，ADAPT同步起练与可选音频反馈；旧技术样机不复用，未BUILD底层引擎。来电/焦点/锁屏仍为模拟，不证明Android后台。20倍快进不是实际训练节奏。

Human2尚未给V4批准；未跨Prototype Approval、Design Freeze或开发授权。
