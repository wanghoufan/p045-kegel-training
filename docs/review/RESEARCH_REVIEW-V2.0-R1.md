# RESEARCH_REVIEW（Phase1 专用；内部 role ID `product-reviewer` 不变）

- Plan Version：`PRODUCT_PLAN_V2.0`（Change C：剂量与进阶体系受控重开）＋配套 `DOSE-EVIDENCE-V2.0`
- Review Round：V2 第 1 轮（R1）；独立审查 A 位（codebuddy/glm-5.3-flash）；本报告独立性成立——未读取任何其他审查者或汇总结论。
- Result：**ACCEPTED_WITH_CONDITIONS**——证据文档抽查基本属实、数值体系全部自洽、非医疗边界落实；但 L6=240 的现实时间跨度未进可测 AC、L5=120 的人群外推表述需收紧、需补一句与官方科普保守口径的关系。附带完成 `PRE-V2-02` 要求的独立复核核心部分（见 Verified Facts）；P0（PRE-V2-01 专业函审）不因本评审闭合。

## P0 / P1 / P2

- P0（未闭，维持）：`PRE-V2-01` 男性盆底专业人员书面函审六级/组数/进阶/疲劳停止/恢复。本评审只做独立研究复核，不替代专业函审。
- P1：`PRE-V2-02`（blocking）——本轮 R1 已完成其点名项的核心独立复核（E10 ≥240 口径、E08 150–180 外推、E13/E14 二手定位、Milios 人群），**建议转「已闭（V2-R1，有条件）」**；条件是残余未读全文（E13/E14/E17/E11）维持降级标注、不升格为独立实证。
- P1（本轮新增，非阻塞）：`R1-P1-01` L6 满量全天跨度未进可测 AC（见 Required Fixes 1）；`R1-P1-02` L5=120 人群外推文案（Required Fixes 2）；`R1-P1-03` 与官方科普「不加量」口径的关系（Required Fixes 3）。
- P2：无新增。

## Key Assumptions（逐条＋是否成立）

- KA-03（重写版：六级剂量/拆分/进阶未经临床验证，240 无督导普通男性长期安全未知）：**成立**。与 Hall 2018（108 项异质、报告欠全）与 Kongtragul 2014（240 仅为依从性阈值）一致。
- KA-01/02/04/05（沿用 V1.4）：维持 V1.4 结论，不因剂量改版改变。

## Verified Facts（已验证事实＋证据；本轮独立取证，非转述 planner）

1. **Kongtragul 2014（E10）逐字核验通过**：Mahidol 机构 PDF 原文（J Med Assoc Thai 2014;97(5)）第 2 页两次出现 "equal or more than 240 times per day, every day, from three weeks to three months after surgery"，为**次要结果（依从性/规律性判定标准）**；样本 135 名根治性前列腺切除术后患者随机分组；两臂＝Kegel 单独 vs Kegel＋专注训练；全文未见快慢拆分、秒数、组数报告。→ V2.0「240 是男性 RCT 依从性标准、非快慢合计配方、非剂量比较」的定性**准确**。
2. **Milios 2019（E05）全文核验通过（对普通用户适用性影响最大的一条）**：Europe PMC 全文（BMC Urol 2019;19:116，PMID 31729959）原文："six sets of PFM exercises per day, with each set comprising 10 fast (1 s duration) and 10 slow (10 s duration) contractions with an equal rest time, providing a total of 120 contractions per day"。**人群＝97 名行根治性前列腺切除术的男性（术前 5 周开始、术后持续 12 周）**；方案在实时超声（RTUS）生物反馈确认动作、每两周物理治疗师随访、训练日记核依从性的条件下执行；对照组 3×10＝30/日。→ **结论：120/日先例来自术后受监督人群，不是普通健康男性自练方案**。V2.0 已如实承认此点，但产品文案层面未收紧（见 Required Fixes 2）。L5=120 锚点是全表外推有效性最弱的一环。
3. **Hall 2018（E07）核验通过**：机构托管 PDF 原文确认 "108 included studies"、"18-240 contractions per day"、"50-51 daily contractions the most common (n=9)"、"83% 未声明收缩强度"、81/108 项为前列腺相关人群；表 4 确含 Franke（文献 53）、Marchiori、Kongtragul（文献 68）。→ 18–240 为研究异质范围、非安全区间，V2.0 定性准确。
4. **Santos 2024 范围综述（E08）核验通过**：SciELO 全文原文："three daily PFMT sessions and … six to 15 contractions per session"为最常见方案；"Performing 50 to 60 contractions per session between the 7th and 26th week of follow-up (2014), will result in a total of 180 contractions per day"。→ 150–180 过渡量确有方案先例，但同样是术后人群，V2.0 已标「量级先例、非均验证安全」，准确。
5. **NIDDK（E01）核验通过，且含反对证据**：官方页确认「保持 3 秒、逐步到每次 10–15 次、每日至少 3 次、完全放松」；同页明确 "Don't overdo it … don't increase how many you do. Overdoing the exercises can lead to straining when you urinate or move your bowels"。→ 官方消费者科普**明确反对增加次数**；本产品高量进阶与其保守口径方向相反，属产品决策，须在文案中如实呈现（见 Required Fixes 3）。
6. **Goode 2011（E12）部分核验**：Europe PMC 确认 208 名持续性术后尿失禁男性 RCT（JAMA 2011，PMID 21224456）；15×3＝45/日剂量位于全文正文，本轮未重读全文，维持 planner 已登记的全文定位。
7. **V2.0 数值体系全部复算通过**：①单次时长 T=6+e(H+R)+f(1+r)+30(gE+gF−1) 复算 L1..L6＝106/144/176/321/476/556 秒，日合计 318/432/528/963/1428/3336 秒（L6=55:36）与表一致；②满量矩阵 D=E+F 全行成立（30/45/60/90/120/240），单次 E+F 与「N×单次=每日量」一致；③FR-005 N 表符合 min(N×40, D)（L5 N=4→120 而非 160，正确封顶于 D）；半量 floor 规则 L2=12+10=22、L6=60+60=120 成立；④L2 选 2 的工作例 q=23/22、E=12/12、F=11/10 复算正确；⑤阶段日额路径与 L6=150 每次示例一致；⑥阈值表/边界例与 V1.4 完全一致未漂移。
8. **对现有实现差距表属实**：`app/src/main/java/com/orca/kegel/domain/Levels.kt:60` `DAILY_CONTRACTION_LIMIT = 40`、`:69` `FREQ3_MAX_ENDURANCE = 6`、`:72` `FREQ3_MAX_TOTAL = 13`，`Plan.kt:88-89` 使用之——与 V2.0「去固定 FREQ3/TOTAL、旧 40 留历史 doseVersion」改造清单一一对应，无虚构。

## External Sources（本轮实际请求）

- Kongtragul 2014 全文 PDF（Mahidol）：https://www2.si.mahidol.ac.th/division/nursing/sins/attachments/article/105/2556_research_complete_02.pdf （HTTP 200，application/pdf，337KB，本轮下载并全文检索）
- Milios 2019 全文（Europe PMC，PMC6858748 fullTextXML）：https://www.ebi.ac.uk/europepmc/webservices/rest/PMC6858748/fullTextXML （HTTP 200）
- Hall 2018 全文 PDF（机构托管）：https://www.pelvichealthinstitute.org/wp-content/uploads/2025/04/Design-of-programs-to-train-pelvic-floor-muscles-in-men-with-urinary-dysfunction-Systematic-review.pdf （HTTP 200，1.3MB，本轮下载并全文检索）
- Santos 2024 范围综述全文（SciELO）：https://www.scielo.br/j/rlae/a/LcgPvtdfpkTqvFjVvNBxcqn/?lang=en （HTTP 200）
- NIDDK Kegel Exercises：https://www.niddk.nih.gov/health-information/urologic-diseases/kegel-exercises （本轮直读）
- Goode 2011（Europe PMC 检索 API，含摘要）：PMID 21224456
- 本地实现：`app/src/main/java/com/orca/kegel/domain/Levels.kt`、`Plan.kt`（只读 grep）

## Counter-evidence（反对证据＋成功的相反做法）

- **NIDDK「don't increase how many you do」**：官方科普明确不建议自行加量；E03（Cleveland）/E04（MedlinePlus）同口径警告过量可致疲劳、紧张、漏尿加重。本产品「升到 240」整体方向与消费者科普相反，只能以「产品设定＋专业函审＋个体确认」支撑，不能宣称「研究最高标准」。
- **240 的性质**：唯一 ≥240 的男性 RCT 记录是**依从性判定线**，不是剂量探索臂；没有任何研究比较 240 vs 更低剂量的获益/风险。把 240 当「目标」而非「上限容忍线」是证据不支持的用法——V2.0 已按「产品计数硬上限、非医学安全保证」处理，正确，必须保持。
- **成功的相反做法（供 Human 参考）**：E12（Goode）显示 45/日已在长期术后失禁人群中作为行为治疗剂量并用维持期降至 15/日；E01–E04 三家科普一致收在 30–45/日。若产品目标偏保守，L1–L3（30–60）已有最广泛共识，L4 以上本质上都是外推实验区。
- **高量人群证据全部是患者**：E05/E07/E08/E10/E12 的人群均为前列腺术后/泌尿功能障碍男性，无一项是普通健康男性剂量研究；「普通用户跟着研究放开」的外推前提不存在。V2.0 的分级解锁（≤45 默认、>45 专业审查、>120 个体指导）是对这一缺口的合理产品化处理，但依赖 PRE-V2-01 落实。

## Unverified Items（未验证项＋验证方法）

1. E13（Marchiori 90/日）、E14（Franke 60/日）：全文未取得（付费/访问失败），数字为 Hall 表 4 二手提取——本轮确认其在 Hall 表 4 中的引用地位，剂量数值本身仍未独立核对；维持降级标注即可（V2.0 已如此标注，诚实）。
2. E17（Serdà 2014）24 周方案细节：仅经 E08 转引（本轮已核实 E08 原文确有 "(2014)" 引用），原 RCT 全文未读。
3. E11（Ye 2003，300–900/日）：未重读；V2.0 已正确不采纳为上限。
4. E02/E03/E04/E09/E15/E16 六源：本轮未逐条重读（抽查覆盖 6/17 主源＋1 部分）；其角色均为背景支持而非决定性数值锚点，风险低。
5. L6 满量的真实依从性（6 会话/日、~55 分钟活跃＋~11 小时跨度）：无任何研究数据，纯产品假设；建议 PRE-V2-03 试练时一并观察。
6. G动等竞品参数：仍未实测（UV-01 沿用），本评审未据竞品做任何判断。

## Required Fixes（给 planner 的可执行修改项；不改被审文件原文，由 planner 修订）

1. **R1-RF-01（P1）满量跨度可测化**：L6 满量 N=6 需要 6 次会话、会话间隔≥2 小时，即使每节 9:16，全天跨度约 10–11 小时——「早上/中午/晚上」三时段模型最多承载 N=3（即 120/日）。要求：FLOW-02/开始页在 L6 选择 N=6 时显示预计跨度与会话时刻分布提示；在 AC-32 或 AC-38 判定中增加「跨度/时刻提示可见且与计算一致」可测条目，避免用户误以为 240 可挤进三个时段。
2. **R1-RF-02（P1）L5/L6 锚点人群表述收紧**：各级标准页与高量门槛文案不得写「120/240 有研究支持」这类无条件表述；必须写明 120 先例人群为「前列腺切除术后、治疗师指导＋超声生物反馈」，普通用户自练属外推。逐条进 AC-59 中英文案清单。
3. **R1-RF-03（P1）与科普保守口径的关系**：合规文案（FR-002 末中文段与 AC-59 清单）补一句：官方科普（如 NIDDK）建议不要自行增加每日次数，本产品的加量进阶是产品设定、且与该保守建议方向相反，是否加量应咨询专业人员。中英双语。
4. **R1-RF-04（记录性）**：在 DOSE-EVIDENCE-V2.0 或 HANDOFF 登记本轮 R1 独立复核回执：E01/E05/E07/E08/E10 已独立取证原文、E12 人群已核；E13/E14/E17/E11 全文仍未读、维持降级；作为 `PRE-V2-02` 闭合（有条件）的凭据。

## Plan Readiness Score（建议分；口径照 PRODUCT_PLAN.template.md）

- 产品目标与用户需求：`19/20`（继承签收版；L6 跨度 UX 缺口 −1）
- 核心方案完整性：`18/20`（数值全自洽、安全闸完整；Milios 适用性文案未收紧 −1、跨度项 −1，与上一行分别计）
- 外部事实与竞品验证：`17/20`（本轮 6 主源独立取证全部通过，较 planner 自评的「待独立复核」上移；4 源全文仍未读 −2、竞品未实测 −1）
- 技术可行性：`13/15`（沿用；差距表属实、无新实现验证）
- 风险与异常场景：`8/10`（新增疲劳/尿流/漏尿出口完整；高量人群证据缺口的残余风险如实）
- 开发范围与 DoD：`6/10`（AC-54..61 可测；R1-RF-01/02/03 对应可测条目待补）
- 未决问题：`1/5`（PRE-V2-01 P0 未闭、Human Approval 未取得）
- **合计：建议 `82/100`**（19+18+17+13+8+6+1）。与 planner 自评分值一致但构成不同：外部事实应上调、核心方案与范围应下调。距 Gate 90 的缺项清单：①PRE-V2-01 专业函收回执（P0）；②R1-RF-01..03 三条可测条目补入 AC；③Human Approval。

## Human-only Decisions（只需用户拍板，TM 汇总后一次提出）

1. 是否接受「L6=240 为产品计数硬上限候选＋三级权限门槛（≤45 默认／>45 专业审查＋个体确认／>120 个体指导）」整套开放方式，并授权启动 PRE-V2-01 专业函审。
2. 45/120 权限分界作为产品审核策略（非临床分界）是否接受。
3. 若接受 240：是否接受「满量需 6 会话、全天分布」的使用形态（约 11 小时跨度），还是把产品上限实际收敛到 N=3（120/日）水平。

## Next Action

回 planner 按 R1-RF-01..04 修订 V2.0（不新增版本号外的变更走 Change C 既有轨道）；修订后仍须 `PRE-V2-01` 专业函审（P0）与 Human Approval 才可进 `READY_FOR_HUMAN_REVIEW`。本评审未看任何其他审查者结论；落盘后已校验文件存在且非空。
