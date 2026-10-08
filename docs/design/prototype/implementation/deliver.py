from pathlib import Path
from html.parser import HTMLParser
import json,hashlib,re,datetime
B=Path(__file__).resolve().parent.parent;D=B.parent
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
u=json.loads((D/'UX_CONTRACT.json').read_text());ds=json.loads((D/'DIRECTION_SET.json').read_text());m=json.loads((B/'PROTOTYPE_MANIFEST.json').read_text());html=(B/'prototype.html').read_text()
class Metadata(HTMLParser):
 def __init__(self):super().__init__();self.values={k:set() for k in ['data-screen-id','data-interaction-id','data-state-id']}
 def handle_starttag(self,tag,attrs):
  for k,v in attrs:
   if k in self.values:self.values[k].add(v)
parser=Metadata();parser.feed(html)
for collection,key,attr in [('screens','screen_id','data-screen-id'),('interactions','interaction_id','data-interaction-id'),('states','state_id','data-state-id')]:
 expected={x[key] for x in u[collection]};assert set(m[collection])==expected;assert parser.values[attr]==expected
cases=set(re.findall(r"case '(INT-\d+)'",(B/'implementation/ui.js').read_text()));assert cases==set(m['interactions'])
assert m['ux_contract']['sha256']==sha(D/'UX_CONTRACT.json')==sha(B/'UX_CONTRACT.json')
assert m['direction_set']['sha256']==sha(D/'DIRECTION_SET.json')==sha(B/'DIRECTION_SET.json')
assert u['selected_direction']['direction_set_version']==ds['direction_version'] and u['selected_direction']['id']==ds['selected_direction']=='A'
assert u['selected_direction']['approval_ref']=='DIRECTION_SET.json#/selection_evidence' and u['selected_direction']['approval_evidence']==ds['selection_evidence']
for p,h in json.loads((B/'input-hashes.json').read_text()).items():assert sha(Path(p))==h, '上游输入被修改:'+p
bindings={'prototype_sha256':sha(B/'prototype.html'),'ux_contract_sha256':sha(D/'UX_CONTRACT.json'),'direction_set_sha256':sha(D/'DIRECTION_SET.json')}
static={'mode':'STATIC_INTEGRITY_NOT_BROWSER_RUNTIME','result':'READY','formal_inputs_valid':True,'metadata_matches_formal_sets':True,'executable_interaction_handlers_complete':True,'screen_ids':len(m['screens']),'interaction_ids':len(m['interactions']),'state_ids':len(m['states']),'protected_inputs_unchanged':True,**bindings,'rule_checks_ref':'static-rule-checks.json','limitations':'语法/规则/元数据检查不能证明HTML实际渲染、按钮被浏览器点击、console无错误或截图可读。'}
(B/'static-integrity.json').write_text(json.dumps(static,ensure_ascii=False,indent=2)+'\n')
record={'record_type':'RUNTIME_ATTEMPT_NOT_SUCCESSFUL_BROWSER_EVIDENCE','prototype_version':'PROTOTYPE-V4.0','created_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),**bindings,'browser_executed':False,'console_inspected':False,'console_blocking_errors':None,'page_errors':None,'interactions_executed':[],'states_observed':[],'screenshots':[],'result':'PENDING_RUNTIME_VALIDATION','attempts':[{'tool':'BrowserOS neo run','action':'列出浏览器页，开工预检','result':'TOOL_APPROVAL_DENIED','actual_output':'MCP tool call requires approval, but approval policy is never'}],'required_rerun':['真实浏览器无等级首页选择L1并开始；有等级今日一次开始。','验证开始同一点击即收紧、无准备/试听/耳机确认；无声音/播放失败字幕继续。','完整训练、暂停恢复、提前结束、焦点中断返回、安全停止与历史。','实际检查console与pageerror，拍可读截图绑定SCR/ST和图片hash。','全部68交互/58状态复核，不能用状态样例替代。'],'user_reported_prior_runtime':{'version':'V3','source':'用户本轮反馈已实点旧版','agent_execution':False,'supports_v4_pass':False},'prior_evidence':'archive-v3及更早目录仅历史，不支持V4运行结论。'}
(B/'runtime-evidence/runtime-attempt.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
(B/'PROTOTYPE_QA.md').write_text((B/'implementation/qa-v4.md').read_text())
(B/'README.md').write_text('# PROTO V4｜Human 2 重试入口\n\n打开 [prototype.html](prototype.html)。有等级点开始立即收紧；无等级明确选L1–L6后立即开始，无准备/试听/耳机确认。摸底可选，可随时跳过。\n\n原生自包含HTML，无需安装或服务。场景面板可清空示例/载入L3；记录隔离。默认系统语音，不可用明确报错。仅模拟提示及20倍快进须明确选择，不能用于实际跟练或证明音频能力。\n\n五槽齐全；静态113项检查通过；runtime待验证；Human 2尚未批准。详情见PROTOTYPE_QA.md。\n')
print('V4 slots=5/5；static=READY；runtime=PENDING；58状态NOT_CAPTURED。')
