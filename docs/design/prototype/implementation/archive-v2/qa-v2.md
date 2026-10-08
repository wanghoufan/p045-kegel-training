# PROTO V2｜可用性整改 QA

EVIDENCE_MODE: NONE
BROWSER_EXECUTED: NO
CONSOLE_INSPECTED: NO
RUNTIME_EVIDENCE_REFS: ["runtime-evidence/runtime-attempt.json"]
PROTOTYPE_RESULT: PENDING_RUNTIME_VALIDATION
PROTOTYPE_GATE: PENDING
INTERACTIVE_PROTOTYPE: PENDING

## 五槽

| 槽 | 交付 |
|---|---|
| HTML | [prototype.html](prototype.html)，自包含移动端可点击模拟器 |
| 资产 | manifest `EMPTY_EXPLICIT`，无外部依赖 |
| manifest | [PROTOTYPE_MANIFEST.json](PROTOTYPE_MANIFEST.json)，16屏/65交互/61状态与UX-V1.2完全一致，当前三份SHA256 |
| 截图索引 | [PROTOTYPE_SCREENSHOT_INDEX.md](PROTOTYPE_SCREENSHOT_INDEX.md)，61条NOT_CAPTURED |
| QA | 本文及[runtime-attempt.json](runtime-evidence/runtime-attempt.json) |

## 已验证

- [static-rule-checks.json](static-rule-checks.json)：86项数值/安全/频率/额度/历史/EHS及内联语法检查通过。
- [static-v2-path-checks.json](static-v2-path-checks.json)：47项实际handler函数级检查通过，覆盖无等级选L1直接准备、有等级一次开始、无摸底结果伪造、五项顺序/X/10即时记录/封顶、跳过、暂停恢复/取消结束、自然完成/历史、六级自选与安全锁。
- [static-integrity.json](static-integrity.json)：HTML三组属性ID与正式契约精确一致，handler全集、版本/方向用户证据及三hash匹配；未获授权修改的输入保持。
- Node使用模拟对象执行函数，不是真实浏览器DOM或点击，不能支持runtime PASS。

## C1–C5

- C1：摸底次级可选，SCR-02/03均可跳过。首页无等级直接选择L1–L6后开始，等级事件来源USER_SELECTED，与摸底建议区分。
- C2/C5：启动即今日；有等级一次开始，内部语音预检直接准备10秒，不强制tab、摸底、试听勾选。无等级不默认赋级。
- C3：完整五项清单、第N/5项、操作及后果说明；完整周期点完成一次即自报确认，即时X/10、最新记录与按压反馈，10次禁用，结束本项才前进。
- C4：常显阶段、两类次数、当前倒计时与总进度；暂停冻结、恢复原剩余、结束保留实际量。批准矩阵/频率/日40额度及安全锁保持。
- 受控语义冲突与用户授权见[变更清单](../PROTOTYPE_V2-变更清单.md)及UX，原Product Plan不改，不新增FR/AC。

## 运行尝试及实际限制

本轮BrowserOS预检被审批策略拒绝：`MCP tool call requires approval, but approval policy is never`。桌面控制getState一次30秒超时，未获得页面。未改QA基础设施、未安装其他浏览器工具、未碰用户主Chrome。

运行尝试JSON绑定当前prototype/UX/Direction三hash。browser=false、console inspected=false、blocking console errors=null、page errors=null；实际点击/观察状态/新截图为空，61状态逐项NOT_CAPTURED。未知错误数不写0。

旧runtime及样例截图已归档`runtime-evidence/archive-v1/`，不能支撑V2结果。

## REQUIRED_RERUN

1. 获准桌面浏览器打开当前HTML，清空示例：首页明确选L1→开始；结束/清空后载入L3→今日一次开始。记录实际点击及ST-56。
2. 摸底依次试五项、计数/X/10/最近记录/10次封顶；第一项及其他项分别跳过回今日；完成建议并采用/暂缓。
3. 完整耐力＋快速、两类专项；暂停/原相位恢复、结束确认/取消、通话/焦点返回后主动恢复；安全异常/额外放松/安全锁，历史和等级来源。
4. 频率1/2/3、日40、6日恢复日、14完成日提示、EHS及错误重试；按manifest全部65交互/61状态要求记录。状态样例不能代替实际路径。
5. 实查console/pageerror，拍真实可读截图；证据JSON绑定三当前hash，每张绑定SCR/ST、相对路径和图片hash，更新索引。完整通过后再同步QA/manifest/runtime为PASS。

## 复用及批准边界

REUSE原生HTML/CSS/DOM/Web Storage/系统语音，ADAPT契约规则与反馈；旧技术样机NOT_REUSED，后续仍REVIEW_FOR_REUSE；BUILD底层引擎未选择。示例空间可清除，不影响真实产品数据。

来电/焦点/锁屏为明确模拟，不证明Android后台能力；仅模拟字幕与20倍快进不证明Audio First。Human 2反馈仅授权整改，尚未批准；本轮没有跨越Prototype Approval、Design Freeze或开发授权。
