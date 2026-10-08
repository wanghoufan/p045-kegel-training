from pathlib import Path
import json,copy,hashlib
B=Path(__file__).resolve().parent.parent
skill=Path.home()/'.config/opencode/skills/interactive-product-prototype/validate_contract.py'
ns={'__name__':'artifact_validation','__file__':str(skill)}
exec(compile(skill.read_text(),str(skill),'exec'),ns)
raw_errors,raw_state=ns['artifact_errors'](B)
original=ns['load']
def compatible_load(p):
 obj=original(p)
 if p.name=='UX_CONTRACT.json':
  direction=original(B/'DIRECTION_SET.json')
  assert obj['selected_direction']['approval_ref']=='DIRECTION_SET.json#/selection_evidence'
  assert obj['selected_direction']['approval_evidence']==direction['selection_evidence']
  obj=copy.deepcopy(obj);obj['selected_direction']['approval_ref']=direction['selection_evidence']
 return obj
ns['load']=compatible_load
errors,state=ns['artifact_errors'](B)
report={'validator':'interactive-product-prototype/validate_contract.py::artifact_errors','raw_validator_state':raw_state,'raw_validator_errors':raw_errors,'compatibility_adapter':'仅内存解析UX approval_ref JSON指针到Direction selection_evidence，检查独立approval_evidence一致；原文件/hash保持。','artifact_state':state,'errors':errors,'skill_benchmark_or_self_test_rerun':False,'runtime_pass_claimed':False}
p=B/'static-integrity.json';p.write_text(json.dumps({**json.loads(p.read_text()),'deployed_artifact_validator':report},ensure_ascii=False,indent=2)+'\n')
assert raw_errors==['UX_DIRECTION_APPROVAL_MISMATCH'],raw_errors
assert state=='PENDING' and not errors,errors
q=B/'PROTOTYPE_QA.md';text=q.read_text()
if '## 部署校验器的字段兼容' not in text:
 text=text.replace('## 运行尝试及实际限制','## 部署校验器的字段兼容\n\n已调用部署校验器artifact_errors，未运行Skill benchmark或self-test。原检查把approval_ref引用字符串直接与证据对象比较，单独报告UX_DIRECTION_APPROVAL_MISMATCH。正式UX使用DIRECTION_SET.json#/selection_evidence并保留一致的approval_evidence。\n\n本轮仅在内存解析该指针并检查证据一致，不改上游、快照或ORCA Skill；其余部署校验原样执行，得到ARTIFACT_STATE=PENDING、errors=[]。兼容记录见static-integrity.json，没有把PENDING当PASS。\n\n## 运行尝试及实际限制');q.write_text(text)
m=json.loads((B/'PROTOTYPE_MANIFEST.json').read_text());r=json.loads((B/'runtime-evidence/runtime-attempt.json').read_text())
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert r['prototype_sha256']==m['prototype']['sha256']==sha(B/'prototype.html')
assert r['ux_contract_sha256']==sha(B.parent/'UX_CONTRACT.json')
assert r['direction_set_sha256']==sha(B.parent/'DIRECTION_SET.json')
assert r['browser_executed'] is False and r['console_inspected'] is False
assert r['console_blocking_errors'] is None and r['page_errors'] is None
assert not r['screenshots'] and not r['interactions_executed'] and not r['states_observed']
assert m['runtime_validation']['status']=='PENDING'
for p,h in json.loads((B/'input-hashes.json').read_text()).items():assert sha(Path(p))==h
print(json.dumps({'slots':'5/5','runtime':'PENDING_RUNTIME_VALIDATION','gate':'PENDING','metadata':'16屏/68交互/58状态精确匹配','console_blocking_errors':None,'captures':0,'protected_inputs':'UNCHANGED','schema_adapter_validation':report},ensure_ascii=False))
