from pathlib import Path
import json,hashlib,re
D=Path(__file__).resolve().parents[2];u=json.loads((D/'UX_CONTRACT.json').read_text());v=u['product_plan'];source=Path('temp/用户草稿-Product Plan V1.1.md')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(source)==v['sha256'];text=source.read_text();valid=set(v['valid_fr_ids']+v['valid_ac_ids'])
assert all(r in text for r in valid)
assert len(v['valid_fr_ids'])==14 and len(v['valid_ac_ids'])==26
s={x['state_id']:x for x in u['states']};i={x['interaction_id']:x for x in u['interactions']};f={x['flow_id']:x for x in u['flows']};sc={x['screen_id']:x for x in u['screens']}
for name,key in [('flows','flow_id'),('screens','screen_id'),('interactions','interaction_id'),('states','state_id')]:
 assert len({x[key] for x in u[name]})==len(u[name])
 for x in u[name]:assert set(x['linked_fr_ids']+x.get('linked_ac_ids',[]))<=valid
fields=['interaction_id','screen_id','linked_fr_ids','linked_ac_ids','trigger','preconditions','system_action','visible_feedback','destination_state','destinations','failure','back_cancel','retry_resume','persistence','failure_destination_states']
for x in i.values():
 assert all(x.get(k) for k in fields),x['interaction_id']
 assert x['screen_id'] in sc and x['destination_state'] in s
 assert all(s[d['state_id']]['screen_id']==d['screen_id'] for d in x['destinations'])
 assert all(st in s for st in x['failure_destination_states']+x.get('pending_destination_states',[]))
 assert not any(re.search(r'TBD|开发自己定|开发者决定',str(x[k])) for k in ['system_action','visible_feedback','failure','back_cancel','retry_resume'])
 assert x['flow_ids'] and all(x['interaction_id'] in f[fid]['interaction_ids'] for fid in x['flow_ids'])
for x in f.values():
 assert x['entry_screen_id'] in sc and x['interaction_ids'] and x['exit_condition']
 assert {e['interaction_id'] for e in x['edges']}==set(x['interaction_ids'])
 for e in x['edges']:assert e['to']==i[e['interaction_id']]['destinations'] and e['from_screen_id']==i[e['interaction_id']]['screen_id']
kinds={'normal','loading','empty','error','permission','offline','interrupted','recovering','success','destructive'}
for x in sc.values():
 assert set(x['state_applicability'])==kinds
 for k,vv in x['state_applicability'].items():assert vv.get('state_ids') or vv.get('handled_by_state_ids') or (vv['applicable'] is False and vv.get('reason'))
 assert set(x['state_ids'])=={st for st,val in s.items() if val['screen_id']==x['screen_id']}
 assert set(x['interaction_ids'])=={st for st,val in i.items() if val['screen_id']==x['screen_id']}
reachable={'SCR-01'}
while True:
 more={d['screen_id'] for x in i.values() if x['screen_id'] in reachable for d in x['destinations']}
 if more<=reachable:break
 reachable|=more
assert set(sc)<=reachable
links=u['explicit_requirement_links'];assert {x['requirement_id'] for x in links}==valid
for l in links:
 assert l['chains']
 for c in l['chains']:
  x=i[c['interaction_id']];assert l['requirement_id'] in x['linked_fr_ids']+x['linked_ac_ids'];assert c['flow_id'] in x['flow_ids'];assert c['screen_id']==x['screen_id'];assert s[c['state_id']]['screen_id']==c['destination_screen_id']
  assert c['state_id'] in ([d['state_id'] for d in x['destinations']] if c['branch']=='destination' else x['failure_destination_states'] if c['branch']=='failure' else x.get('pending_destination_states',[]))
dir=json.loads((D/'DIRECTION_SET.json').read_text());assert u['selected_direction']['id']==dir['selected_direction']=='A';assert u['selected_direction']['approval_evidence']==dir['selection_evidence'];assert sha(D/'DIRECTION_SET.json')==u['selected_direction']['sha256']
assert all(x['status']=='APPROVED' and x.get('approved_value') and x['resolution_ref']=='docs/design/PRODUCT_RULES_APPROVED.md' for x in u['required_product_policy'].values())
assert not [x for x in u['unresolved'] if x['blocking']];assert all(x.get('resolved') for x in u['unresolved'])
assert u['controlled_semantic_updates']['status']=='USER_AUTHORIZED';assert u['gate']=='PASS' and u['status']=='UX_INTERACTION_PASS'
report={'result':'PASS','mode':'CONTRACT_STRUCTURAL_VALIDATION_NOT_RUNTIME','ux_contract_sha256':sha(D/'UX_CONTRACT.json'),'source_sha256':sha(source),'source_unchanged':True,'fr_count':14,'ac_count':26,'flow_count':len(f),'screen_count':len(sc),'interaction_count':len(i),'state_count':len(s),'requirement_chains':sum(len(x['chains']) for x in links),'unknown_requirement_ids':[],'controlled_semantic_changes':['V4_ZERO_PRECONDITION_START'],'human2_approved':False}
(D/'prototype/ux-v4-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(report)
