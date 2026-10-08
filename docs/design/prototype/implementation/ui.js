'use strict';
const KEY='human2-voice-coach-simulator-v1';
let db=Domain.initial(),current={screen:'SCR-01',state:'ST-03'},storageOK=true,message='',assessment=null,step=0,result=null,planDraft=null,holdStart=null,holdValue=0,holdRunning=false,historyRange='all',ehsValue=null,ehsConsent=false,speed=1,captionOnly=false,externalInterrupt=false,simulatedBackground=false,offlineOverlay=false,failNext=false,extensionRemaining=0,extensionFrozen=false,preview=false,lastTick=performance.now();
let inputDraft={},inputDraftScreen=null,latestAssessmentRecord='',voiceToken=0,audioNotice='',rulesFault=false,useApprovedFallback=false;
const log={interactions:[],states:[],events:[],errors:[]};
const $=sel=>document.querySelector(sel),escapeHTML=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const uid=()=>typeof crypto.randomUUID==='function'?crypto.randomUUID():`${Date.now()}-${Math.random().toString(36).slice(2)}`;
const stateById=Object.fromEntries(CONTRACT.states.map(s=>[s.state_id,s]));
const interactionById=Object.fromEntries(CONTRACT.interactions.map(i=>[i.interaction_id,i]));
function observed(){log.states.push(current.state);log.events.push({at:new Date().toISOString(),type:'state',screen_id:current.screen,state_id:current.state,preview});}
function go(state,msg=''){const s=stateById[state];if(!s)throw new Error('未知状态');current={screen:s.screen_id,state};message=msg;observed();render();}
function stopVoice(){voiceToken++;if('speechSynthesis'in window)speechSynthesis.cancel();}
function audioUnavailable(reason='当前声音不可用'){
 audioNotice=reason+'；字幕和训练节奏继续，耳机由你自行选择。';
 if(db.active&&!db.active.paused)go('ST-21',audioNotice);
 else {message=audioNotice;render();}
}
function say(text){
 $('#spoken').textContent=text||'（本阶段仅界面提示）';
 if(!text){stopVoice();return true;}
 if(captionOnly||speed!==1)return true;
 if(!('speechSynthesis'in window)||!('SpeechSynthesisUtterance'in window))return false;
 try{
  stopVoice();const token=voiceToken;const utterance=new SpeechSynthesisUtterance(text);utterance.lang='zh-CN';utterance.rate=1.6;
  utterance.onerror=e=>{if(token===voiceToken&&!['interrupted','canceled'].includes(e.error))audioUnavailable('语音播放失败');};
  speechSynthesis.speak(utterance);return true;
 }catch(e){log.events.push({type:'optional_audio_error',message:String(e)});return false;}
}
function note(id,arg){log.interactions.push(id);log.events.push({at:new Date().toISOString(),type:'interaction',interaction_id:id,screen_id:current.screen,state_id:current.state,arg:typeof arg==='string'?arg:''});}
function fail(id){stopVoice();if(db.active)db.active.paused=true;holdRunning=false;const i=interactionById[id];go((i?.failure_destination_states||['ST-22'])[0],'模拟本地读写失败：原数据保持，请主动重试。');}
function persist(id){if(failNext){failNext=false;fail(id);return false;}try{localStorage.setItem(KEY,JSON.stringify(db));storageOK=true;return true;}catch(e){storageOK=false;log.errors.push({kind:'storage',message:String(e)});fail(id);return false;}}
function change(id,fn){const before=JSON.stringify(db);fn();if(!persist(id)){db=JSON.parse(before);return false;}return true;}
function pending(st,fn){go(st);setTimeout(fn,250);}
function btn(id,label,arg='',opts={}){const t=$('#control-'+id).content.firstElementChild.cloneNode(true);t.textContent=label||interactionById[id].trigger;if(arg!=='')t.dataset.value=arg;if(opts.secondary)t.classList.add('secondary');if(opts.danger)t.classList.add('danger');if(opts.disabled)t.disabled=true;return t.outerHTML;}
function extra(label,action,value=''){return `<button class="secondary" data-review-action="${action}" data-value="${value}">${escapeHTML(label)}</button>`;}
function row(label,value){return `<div class="row"><span>${label}</span><strong>${escapeHTML(value)}</strong></div>`;}
function field(label,id,value='',type='number',attrs=''){return `<label class="field">${label}<input id="${id}" type="${type}" value="${escapeHTML(value)}" ${attrs}></label>`;}
function select(label,id,options,value){return `<label class="field">${label}<select id="${id}">${options.map(([v,l])=>`<option value="${v}" ${value===v?'selected':''}>${l}</option>`).join('')}</select></label>`;}
function check(label,id,value=false){return `<label class="check"><input id="${id}" type="checkbox" ${value?'checked':''}>${label}</label>`;}
function safetyButton(id){return `<details class="safety-actions"><summary>身体不适？</summary><p>感觉异常就停下来。</p>${btn(id,'疼痛或持续不适','PAIN',{danger:true})}${btn(id,'明显排尿 / 排便困难','SYMPTOM',{danger:true})}${btn(id,'无法完全放松','RELAX',{secondary:true})}</details>`;}
function newAssessment(){assessment={id:uid(),day:Domain.date(),control:'GOOD',seconds:null,endurance:0,fast:0,relaxation:'GOOD',status:'DRAFT',symptom:null};step=0;result=null;holdRunning=false;holdValue=0;latestAssessmentRecord="";}
function assessmentSave(status){assessment.status=status;const a=JSON.parse(JSON.stringify(assessment));if(!change('INT-07',()=>{const old=db.assessments.findIndex(x=>x.id===a.id);if(old>=0)db.assessments[old]=a;else db.assessments.push(a);})){return false;}return true;}
function safe(reason='PAIN'){
 stopVoice();holdRunning=false;holdStart=null;db.safetyLock=false;db.trainingEnabled=true;db.lowerPending=false;
 if(assessment&&['SCR-02','SCR-03','SCR-04'].includes(current.screen)){
  db.lastAssessment={id:assessment.id,level:null,reason:'NO_LEVEL_'+reason,day:assessment.day,createdAt:new Date().toISOString()};assessment.symptom=reason;assessment.status='SAFETY_ABORT';assessment.reason='NO_LEVEL_'+reason;
  const copy=structuredClone(assessment),idx=db.assessments.findIndex(a=>a.id===copy.id);
  if(idx>=0)db.assessments[idx]=copy;else db.assessments.push(copy);
 }
 if(db.active){db.active.paused=true;db.active.status='SAFETY_ABORT_'+reason;}
 else if(current.screen==='SCR-10'&&db.sessions.length){const last=db.sessions.at(-1);last.status='SAFETY_ABORT_'+reason;last.valid=false;}
 const iid={'SCR-02':'INT-41','SCR-03':'INT-42','SCR-08':'INT-44','SCR-10':'INT-45'}[current.screen]||'INT-32';
 const saved=persist(iid);go('ST-25',(reason==='RELAX'?'无法完全放松：本次已停止。':'出现不适：本次已停止。')+(saved?'':'停止已生效，但安全状态未能保存；不要重开训练。'));
}
function ability(){go(db.level?'ST-08':'ST-29');}
function dose(mode){
 if(db.active){go('ST-15','已有训练，请主动恢复或结束。');return;}
 if(rulesFault||(!useApprovedFallback&&!Domain.matrixValid(POLICY.six_level_parameters_and_test_cadence.training_matrix))){go('ST-28','当前规则配置错误，可手动选级使用内置批准矩阵继续。');return;}
 if(!db.level){go('ST-60','请选择起始等级；摸底是可选入口。');return;}
 if(db.restChoices[Domain.date()]==='REST_DAY'){go('ST-47','今天已选择休息。');return;}
 planDraft=Domain.plan(db,mode,db.restChoices[Domain.date()]==='HALF_VOLUME',useApprovedFallback?APPROVED_MATRIX:undefined);if(!planDraft){go('ST-47');return;}
 startTraining('INT-12');
}
function startTraining(id){
 if(db.active){go('ST-15','已有会话，未重复创建。');return;}
 planDraft=Domain.plan(db,planDraft?.mode||'COMBINED',db.restChoices[Domain.date()]==='HALF_VOLUME',useApprovedFallback?APPROVED_MATRIX:undefined);if(!planDraft){go('ST-47');return;}
 const first=planDraft.E>0?'E':'F';audioNotice='';
 if(!change(id,()=>{db.active={id:uid(),day:Domain.date(),plan:structuredClone(planDraft),levelSource:db.levelSource||'ASSESSMENT',phase:first,remaining:first==='E'?planDraft.hold_seconds:planDraft.fast_seconds,elapsedLogical:0,doneE:0,doneF:0,paused:false,status:null,relaxReports:0,extensionPending:false,startedAt:new Date().toISOString()};Domain.quota(db,`${db.active.id}:${first}:0`);}))return;
 lastTick=performance.now();go(phaseState(),'已开始训练，首个收紧立即执行。');cue();
}
function assessmentProgress(n){const names=['动作控制','耐力保持（秒）','耐力次数','快速次数','放松质量'];return `<div class="assessment-progress"><strong>第${n+1}/5项 · ${names[n]}</strong><div class="track"><i style="width:${(n+1)*20}%"></i></div><ol>${names.map((name,i)=>`<li class="${i===n?'current':''}">${i+1}. ${name}</li>`).join('')}</ol></div>`;}
function trainingHUD(s){const p=s?.plan||{E:0,F:0,duration:1},ph=s?.phase||'E';const stage=ph==='E'?'耐力':['ER','FR','GROUP'].includes(ph)?'休息':ph==='F'?'快速':'结束';const pct=Math.min(100,Math.floor(100*(s?.elapsedLogical||0)/p.duration));return `<div class="training-hud"><div class="stage-chain">${['耐力','休息','快速','结束'].map(x=>`<span class="${x===stage?'active':''}">${x}</span>`).join('')}</div><div class="grid2"><div>耐力 <strong>${s?.doneE||0}/${p.E}</strong></div><div>快速 <strong>${s?.doneF||0}/${p.F}</strong></div></div><div class="row"><span>总进度 <strong id="totalPercent">${pct}%</strong></span><span>完整周期 ${(s?.doneE||0)+(s?.doneF||0)}/${p.E+p.F}</span></div><div class="track"><i id="totalBar" style="width:${pct}%"></i></div></div>`;}
function pause(kind='ST-14',reason=''){stopVoice();if(db.active)db.active.paused=true;if(!persist(kind==='ST-15'?'INT-20':'INT-19'))return;go(kind,reason);}
function interrupt(reason='来电 / 音频焦点丢失'){externalInterrupt=true;pause('ST-15',reason);}
function phaseState(){const p=db.active?.phase;return p==='END'?'ST-57':p==='E'?'ST-11':p==='F'?'ST-13':'ST-12';}
function cue(){const s=db.active;if(!s)return;const fastOnly=s.plan.mode==='FAST';const text={E:'收紧',F:'紧',ER:'放松',FR:'松',GROUP:'休息',END:fastOnly?'':'结束'}[s.phase];if(!say(text))audioUnavailable();}
function phase(p,seconds){const s=db.active;s.phase=p;s.remaining=seconds;s.paused=false;lastTick=performance.now();if(!persist('INT-18')){s.paused=true;return;}go(phaseState());cue();}
function contraction(type){const s=db.active;const index=type==='E'?s.doneE:s.doneF;const key=`${s.id}:${type}:${index}`;if(!Domain.quota(db,key)){s.status='INCOMPLETE_BUDGET';endSession();return;}phase(type,type==='E'?s.plan.hold_seconds:s.plan.fast_seconds);}
function endSession(status){stopVoice();holdRunning=false;const s=db.active;if(!s){ability();return;}s.paused=true;s.status=status||s.status||'COMPLETED';s.partial={phase:s.phase,remainingSeconds:s.remaining,enduranceIncomplete:['E','ER'].includes(s.phase)?1:0,fastIncomplete:['F','FR'].includes(s.phase)?1:0};go('ST-18');setTimeout(()=>act('INT-25'),250);}
function advance(){const s=db.active;if(!s||s.paused)return;const node=document.querySelector('#phase-event [data-interaction-id]');if(node)act(node.dataset.interactionId);}

function resume(){const s=db.active;if(!s)return;if(s.extensionPending){go('ST-44');return;}if(s.status?.startsWith('SAFETY_ABORT')){go('ST-25','本次已安全停止，只能结束，不能恢复。');return;}if(externalInterrupt){go('ST-15','中断原因尚未解除。请先模拟返回，再主动恢复。');return;}pending('ST-16',()=>{s.paused=false;lastTick=performance.now();if(!persist('INT-21')){s.paused=true;return;}go(phaseState(),'已恢复，同一阶段继续剩余秒数。');if(s.remaining===0){advance();return;}cue();});}
function act(id,arg=''){inputDraftScreen=current.screen;inputDraft=Object.fromEntries([...document.querySelectorAll('.screen:not([hidden]) input, .screen:not([hidden]) select')].filter(e=>e.id).map(e=>[e.id,{value:e.value,checked:e.checked}]));preview=false;note(id,arg);if(failNext){failNext=false;fail(id);return;}
 switch(id){
 case 'INT-01':if(db.active)go('ST-15','上次训练尚未结束；主动恢复或结束。');else go(db.level?'ST-01':'ST-02');break;
 case 'INT-02':if(db.active){go('ST-15');break;}newAssessment();go('ST-04');break;
 case 'INT-03':if(!assessment)newAssessment();assessment.control=$('#controlQuality')?.value||assessment.control;if(assessment.control!=='GOOD'){safe('CONTROL');break;}step=1;latestAssessmentRecord='动作控制已确认';go('ST-05','动作控制已记录；接下来测耐力保持（秒）。');break;
 case 'INT-04':if(arg==='start'){holdStart=performance.now();holdRunning=true;holdValue=0;render();break;}if(arg==='stop'){holdValue=Math.min(10,(performance.now()-holdStart)/1000);holdRunning=false;holdStart=null;render();break;}const seconds=Number($('#holdSeconds')?.value||holdValue);if(!Number.isFinite(seconds)||seconds<1||seconds>10){go('ST-06','有效保持须在1–10秒，不足1秒不能强记1秒。');break;}assessment.seconds=Math.floor(seconds);latestAssessmentRecord=`耐力保持已记录 ${assessment.seconds} 秒`;step=2;go('ST-05',latestAssessmentRecord);break;
 case 'INT-05':case 'INT-06':{const type=id==='INT-05'?'endurance':'fast';if(arg==='finish'){step=id==='INT-05'?3:4;go('ST-05',`${id==='INT-05'?'耐力':'快速'}次数已确认 ${assessment[type]}/10，进入下一项。`);break;}if(assessment[type]>=10){go('ST-05','已记录10次，请结束本项。');break;}assessment[type]++;latestAssessmentRecord=`${id==='INT-05'?'耐力':'快速'}第 ${assessment[type]} 次已记录：已收紧并完全放松`;go('ST-05',latestAssessmentRecord);break;}
 case 'INT-07':assessment.relaxation=$('#relaxationQuality')?.value||'GOOD';if(assessment.relaxation!=='GOOD'){assessmentSave('SAFETY_ABORT');safe('RELAX');break;}if(assessment.seconds===null){go('ST-06','摸底尚未完整。');break;}if(!assessmentSave('COMPLETE'))break;pending('ST-32',()=>act('INT-09'));break;
 case 'INT-08':holdRunning=false;holdStart=null;if(arg==='back'&&step>0){step--;go(step===0?'ST-04':'ST-05','已返回上一项，已记录输入保留。');break;}assessment=null;go(db.level?'ST-01':'ST-02','本次摸底已取消，可直接训练。');break;
 case 'INT-09':if(!assessment){go('ST-34','没有本次完整摸底，请自愿重新测试。');break;}result=Domain.recommend(db,assessment);if(!change(id,()=>{db.lastAssessment={id:assessment.id,level:result.level,reason:result.reason,day:assessment.day,createdAt:new Date().toISOString()};const record=db.assessments.find(x=>x.id===assessment.id);if(record)record.referenceLevel=result.level;}))break;if(result.reason.startsWith('NO_LEVEL_')){safe(result.reason.split('_').pop());break;}go(result.level?'ST-07':'ST-46','摸底结果已保存；不改变当前选择。');break;
 case 'INT-10':if(!result?.level)break;if(!change(id,()=>Domain.selectStartingLevel(db,result.level,BINDINGS.ux_contract_version)))break;go('ST-01','已使用本次摸底作为当前选择，仍可随时改级。');break;
 case 'INT-11':if(assessment)assessmentSave('NOT_APPLIED');ability();break;
 case 'INT-12':if(db.active){go('ST-15');break;}dose('COMBINED');break;
 case 'INT-13':if(db.active){go('ST-15');break;}dose(arg==='FAST'?'FAST':'ENDURANCE');break;
 case 'INT-16':phase('ER',db.active.plan.rest_seconds);break;
 case 'INT-17':phase('FR',db.active.plan.fast_rest_seconds);break;
 case 'INT-18':{const s=db.active;if(!s)break;if(s.phase==='ER'){s.doneE++;if(s.doneE<s.plan.E)contraction('E');else if(s.plan.F>0)phase('GROUP',10);else phase('END',10);}else if(s.phase==='FR'){s.doneF++;if(s.doneF<s.plan.F)contraction('F');else phase('END',10);}else if(s.phase==='GROUP')contraction('F');break;}
 case 'INT-19':pause();break;
 case 'INT-20':interrupt(arg||'来电 / 音频焦点丢失');break;
 case 'INT-21':resume();break;
 case 'INT-22':stopVoice();if(db.active)db.active.paused=true;persist(id);go('ST-17');break;
 case 'INT-23':{const s=db.active;if(s){const full=s.doneE===s.plan.E&&s.doneF===s.plan.F;endSession(full?'COMPLETED':externalInterrupt?'INCOMPLETE_FOCUS':'INCOMPLETE_MANUAL');}break;}
 case 'INT-24':go('ST-14','已取消结束；训练仍暂停。');break;
 case 'INT-25':{const s=db.active;if(!s){go('ST-19');break;}if(!change(id,()=>{const record={...structuredClone(s),endedAt:new Date().toISOString(),valid:true};record.status=s.status||'COMPLETED';if(!db.sessions.some(x=>x.id===s.id))db.sessions.push(record);db.active=null;}))break;go('ST-19','已保存在此浏览器演示空间。');break;}
 case 'INT-26':if(!change(id,()=>{const last=db.sessions.at(-1);if(last)last.valid=$('#qualityValid')?.checked??true;}))break;go('ST-19','自报质量已更新；日额度不回退。');break;
 case 'INT-27':pending('ST-40',()=>go(db.sessions.length||db.assessments.length?'ST-20':'ST-24'));break;
 case 'INT-28':historyRange=$('#historyRange')?.value||'all';go(historyRange==='empty'?'ST-24':'ST-20');break;
 case 'INT-29':pending('ST-35',()=>{ability();if(Domain.completedDays(db).length>=14&&!db.promptShown)act('INT-31');});break;
 case 'INT-30':if(db.active){go('ST-15');break;}newAssessment();go('ST-04','重新摸底不会覆盖旧记录。');break;
 case 'INT-31':if(!change(id,()=>{db.promptShown=true;}))break;go('ST-08','已完成14个训练日，可以重新摸底；可忽略，不自动升级。');break;
 case 'INT-32':case 'INT-44':if(arg==='RELAX'&&db.active&&['ER','FR','GROUP'].includes(db.active.phase)&&db.active.relaxReports===0){stopVoice();db.active.relaxReports=1;db.active.extensionPending=true;db.active.paused=true;extensionRemaining=10;db.active.extensionRemaining=10;extensionFrozen=false;lastTick=performance.now();if(!persist(id))break;go('ST-44');say(db.active?.phase==='FR'?'松':'放松');break;}safe(arg==='SYMPTOM'?'SYMPTOM':arg==='RELAX'?'RELAX':'PAIN');break;
 case 'INT-33':if(db.active)endSession(db.active.status||'SAFETY_ABORT_PAIN');else ability();break;
 case 'INT-34':simulatedBackground=!simulatedBackground;go(phaseState(),simulatedBackground?'锁屏/后台界面演示。此HTML不能验证Android后台能力。':'回到前台，仍为同一相位。');break;
 case 'INT-35':if(db.active){stopVoice();db.active.paused=true;if(!persist(id))break;extensionRemaining=db.active.extensionRemaining??10;extensionFrozen=!!db.active.extensionPending;go('ST-15','进程重开场景：退出期间量不补算。');}else go('ST-02');break;
 case 'INT-36':if(!change(id,()=>{db.notificationChoice=arg||'DENIED';}))break;go('ST-30',arg==='ALLOWED'?'通知许可已模拟允许。':'通知许可已模拟拒绝，不阻止前台训练。');break;
 case 'INT-37':pending('ST-42',()=>go('ST-30'));break;
 case 'INT-38':{const n=Number($('#frequency')?.value);if(![1,2,3].includes(n)){go('ST-43');break;}if(!change(id,()=>{db.frequency=n;}))break;go('ST-30','提醒频率已保存；当前会话与已用额度不变。');break;}
 case 'INT-39':offlineOverlay=!offlineOverlay;go(offlineOverlay?'ST-27':phaseState(),offlineOverlay?'已模拟断网，本地训练仍按原相位推进。':'网络恢复，不补播旧阶段。');break;
 case 'INT-40':if(assessment&&arg==='calculate'){pending('ST-32',()=>act('INT-09'));break;}if(planDraft&&arg==='prepare'){dose(planDraft.mode||'COMBINED');break;}ability();break;
 case 'INT-41':case 'INT-42':case 'INT-45':safe(arg||'PAIN');break;
 case 'INT-46':go('ST-49');break;
 case 'INT-47':pending('ST-58',()=>{if(change(id,()=>{db.ehsEnabled=true;}))go('ST-49','已自愿启用，仍可跳过。');});break;
 case 'INT-48':ehsValue=Number($('#ehsValue')?.value);ehsConsent=!!$('#ehsConsent')?.checked;if(!db.ehsEnabled||![1,2,3,4].includes(ehsValue)||!ehsConsent){go('ST-52','请自愿启用、选1–4并确认隐私说明。');break;}pending('ST-58',()=>{if(change(id,()=>{db.ehs.push({id:uid(),day:Domain.date(),value:ehsValue});}))go('ST-51','已保存独立自评，与训练等级无关。');});break;
 case 'INT-49':ehsValue=null;ehsConsent=false;ability();break;
 case 'INT-50':case 'INT-58':pending('ST-59',()=>go(db.ehs.length?'ST-50':'ST-53'));break;
 case 'INT-51':pending('ST-58',()=>{if(change(id,()=>{db.ehsEnabled=false;}))go('ST-49','已关闭；已有记录本地保留，可单独清除。');});break;
 case 'INT-52':go('ST-55');break;
 case 'INT-53':pending('ST-59',()=>{if(change(id,()=>{db.ehs=[];}))go('ST-53','已单独清除EHS；训练记录、等级、额度不变。');});break;
 case 'INT-54':go(db.ehs.length?'ST-50':'ST-53');break;
 case 'INT-55':if(arg==='END'||arg==='STILL'){safe('RELAX');break;}if(extensionRemaining>0||extensionFrozen){go('ST-44','请完成额外10秒放松，不能提前进入下一次。');break;}if(db.active){db.active.extensionPending=false;db.active.paused=false;lastTick=performance.now();persist(id);go('ST-12');cue();}break;
 case 'INT-56':if(arg==='CANCEL'){go('ST-48');break;}if(!change(id,()=>{db.restChoices[Domain.date()]=arg==='REST_DAY'?'REST_DAY':'HALF_VOLUME';}))break;go('ST-01',arg==='REST_DAY'?'今天休息，不计训练完成。':'已选择半量，日额度仍共享。');break;
 case 'INT-57':go('ST-49');break;
 case 'INT-60':endSession('COMPLETED');break;
 case 'INT-61':if(db.active?.extensionPending){act('INT-55','END');break;}go('ST-17');break;
 case 'INT-62':go('ST-60','自由选择L1–L6；上次摸底仅供了解。');break;
 case 'INT-63':{const level=Number(arg||$('#startingLevel')?.value);if(!Number.isInteger(level)||level<1||level>6){go('ST-60','请选择L1–L6中的一个等级。');break;}if(!change(id,()=>Domain.selectStartingLevel(db,level,BINDINGS.ux_contract_version)))break;dose('COMBINED');break;}
 case 'INT-64':case 'INT-65':holdRunning=false;holdStart=null;assessment=null;step=0;go(db.level?'ST-01':'ST-02','已跳过本次摸底，没有生成摸底结果。');break;
 case 'INT-66':case 'INT-67':go('ST-62');break;
 case 'INT-68':go(db.level?'ST-01':'ST-02','查看标准不改当前选择。');break;
 case 'INT-69':{const level=Number(arg||$('#startingLevel')?.value);if(!Number.isInteger(level)||level<1||level>6){go('ST-60','请明确选择L1–L6。');break;}if(!change(id,()=>Domain.selectStartingLevel(db,level,BINDINGS.ux_contract_version)))break;go('ST-01',`当前选择已保存 L${level}；上次摸底保持独立。`);break;}
 case 'INT-70':{const level=Number(arg||$('#fallbackLevel')?.value);if(!Number.isInteger(level)||level<1||level>6){go('ST-60','请选择有效等级。');break;}if(!change(id,()=>Domain.selectStartingLevel(db,level,BINDINGS.ux_contract_version)))break;rulesFault=false;useApprovedFallback=true;dose('COMBINED');break;}
 case 'INT-71':case 'INT-72':{const next=Number(arg);if(!Number.isInteger(next)||next<1||next>6){message='请选择L1–L6';render();break;}const previous=current.state;if(!change(id,()=>Domain.selectStartingLevel(db,next,BINDINGS.ux_contract_version)))break;go(previous,`当前选择 L${next} 已保存；本次仍按 L${db.active?.plan.level} 快照，阶段不变。`);break;}
 default:throw new Error('未实现交互 '+id);
 }
}
function nav(){return btn('INT-01','今日','',{secondary:true})+btn('INT-29','能力','',{secondary:true})+btn('INT-27','记录','',{secondary:true});}
function parameters(p){return `<div class="grid2"><div class="metric">耐力<strong>${p.E??p.endurance_reps}次</strong><small>保持${p.hold_seconds}秒 / 放松${p.rest_seconds}秒</small></div><div class="metric">快速<strong>${p.F??p.fast_reps}次</strong><small>收紧${p.fast_seconds}秒 / 放松${p.fast_rest_seconds}秒</small></div></div>`;}
function phaseLabel(p){return ({E:'耐力收紧 / 保持',ER:'放松',F:'紧',FR:'松',GROUP:'组间休息',END:'结束提示'})[p]||'无活动会话';}
function statusLabel(s){return ({COMPLETED:'完整训练',INCOMPLETE_MANUAL:'手动提前结束',INCOMPLETE_FOCUS:'中断后结束',INCOMPLETE_BUDGET:'日额度结束',SAFETY_ABORT_PAIN:'疼痛安全停止',SAFETY_ABORT_RELAX:'无法放松安全停止',SAFETY_ABORT_SYMPTOM:'异常症状安全停止',APPLIED:'已采用建议',NOT_APPLIED:'未采用建议',COMPLETE:'摸底已保存',SAFETY_ABORT:'摸底安全停止',DRAFT:'摸底草稿'})[s]||'未完成';}
function title(k,h,p=''){return `<div class="eyebrow">${k}</div><h1>${h}</h1>${p?`<p class="muted">${p}</p>`:''}`;}
function levelChoice(id='INT-69'){return select(id==='INT-69'?'当前选择（随时可改）':'下次训练选择（本次等级快照保持）','startingLevel',[['','请选择等级'],...[1,2,3,4,5,6].map(n=>[n,`L${n}`])],db.level||'').replace('<select id="startingLevel"',`<select data-interaction-id="${id}" id="startingLevel"`);}
function levelInfo(){return `<div class="level-info">${row('上次摸底',db.lastAssessment?db.lastAssessment.level?`L${db.lastAssessment.level}`:'暂无有效等级':'尚未摸底')}${row('当前选择',db.level?`L${db.level}`:'未选择')}${row('默认建议（可改）',db.lastAssessment?.level?`L${db.lastAssessment.level}`:'暂无参考')}<p class="hint">摸底仅作了解，不锁等级；当前选择独立保存。</p></div>`;}
function standards(){const fmt=x=>`${Math.floor(x/60)}分${x%60}秒`;return title('各级标准','知道节奏，再选目标','训练等级＝产品节奏，非医学肌力、非诊断。')+`<p class="hint">单次直接收紧开始，含组间10秒、结束10秒，无准备等待；默认每天2次。下面另按当前${db.frequency}次提醒估算；3次自动降量，日收缩不超过40。实际耗时可能含暂停/额外休息，起练不等待语音。恢复日/剩余日额度会调整实际量。</p>`+APPROVED_MATRIX.map((m,i)=>{const standard=Domain.plan({...Domain.initial(),level:i+1,frequency:2},'COMBINED',false,APPROVED_MATRIX);const selected=Domain.plan({...Domain.initial(),level:i+1,frequency:db.frequency},'COMBINED',false,APPROVED_MATRIX);return `<section class="standard-card"><h2>L${i+1}</h2><table><thead><tr><th>类型</th><th>收紧</th><th>放松/间隔</th><th>次数</th></tr></thead><tbody><tr><td>耐力</td><td>${m.hold_seconds}秒</td><td>${m.rest_seconds}秒</td><td>${m.endurance_reps}次</td></tr><tr><td>快速</td><td>${m.fast_seconds}秒</td><td>${m.fast_rest_seconds}秒</td><td>${m.fast_reps}次</td></tr></tbody></table><p>单次约${fmt(standard.duration)} · ${standard.E+standard.F}次收缩<br>默认每日2次：约${fmt(standard.duration*2)} · ${(standard.E+standard.F)*2}次<br>当前每日${db.frequency}次：单次${selected.E}耐力＋${selected.F}快速，日约${fmt(selected.duration*db.frequency)} · ${(selected.E+selected.F)*db.frequency}次收缩</p></section>`;}).join('')+btn('INT-68','返回今日','',{secondary:true});}
function contents(){
 const st=current.state,sc=current.screen,s=db.active,stat=Domain.stats(db),used=Domain.used(db),level=db.level?`L${db.level}`:'无训练等级';
 if(sc==='SCR-01'){
  let out=title('随身语音教练','今日训练','练习收紧，也练习完全放松。');
  if(st==='ST-48')return out+`<div class="card"><h2>给自己一个恢复日</h2><p>已经连续完成6天。今天可以休息或半量，这不是医学标准。</p>${btn('INT-56','今天休息','REST_DAY')}${btn('INT-56','选择半量','HALF_VOLUME',{secondary:true})}${btn('INT-56','暂不选择','CANCEL',{secondary:true})}</div>`;
  if(Domain.streak(db)>=6&&st!=='ST-48')out+=`<p class="hint">连续完成6日，可选择恢复日；不会自动拦住今天开始。</p>${btn('INT-56','看看休息 / 半量选择','CANCEL',{secondary:true})}`;
  if(st==='ST-47')out+=`<div class="card"><h2>今天可以休息</h2><p>已用${used} / 40次，剩余${40-used}次。今日组合至少需要1次耐力＋1次快速。</p>${btn('INT-29','查看能力与专项','',{secondary:true})}</div>`;
  const p=Domain.plan(db,'COMBINED',db.restChoices[Domain.date()]==='HALF_VOLUME',useApprovedFallback?APPROVED_MATRIX:undefined);
  out+=`<div class="hero-plan"><h2>耐力 + 快速</h2>${levelInfo()}${levelChoice()}${db.level?btn('INT-12','开始今日训练'):btn('INT-63','开始今日训练')}<p>${p?`约${Math.floor(p.duration/60)}分${p.duration%60}秒 · ${p.E}次耐力＋${p.F}次快速`:'明确选择L1–L6即可开始；摸底全程可选。'}</p>${p?parameters(p):''}${btn('INT-62','查看 / 更改等级','',{secondary:true})}${btn('INT-66','各级标准','',{secondary:true})}</div>`;
  out+=`<div class="secondary-entry">${btn('INT-02',db.assessments.length?'可选：再次摸底':'可选：首次摸底','',{secondary:true})}${btn('INT-29','查看能力','',{secondary:true})}${btn('INT-27','查看记录','',{secondary:true})}</div>`;
  out+=`<div class="row"><span>每日产品提醒</span><strong>${db.frequency}次</strong></div><div class="row"><span>今日已引导收缩</span><strong>${used} / 40</strong></div><div class="row"><span>完成训练日</span><strong>${stat.days}天</strong></div>${btn('INT-37','提醒与自评设置','',{secondary:true})}<p class="hint">这是产品提醒，不是医学处方。按身体感受休息，不必为打卡增加训练。</p>`;return out;
 }
 if(sc==='SCR-02')return assessmentProgress(0)+title('可选摸底','先找到正确动作','向内、向上收紧，正常呼吸，不夹臀腿、不向下用力。')+`<div class="card"><p>在做什么：确认你能识别收紧与完全放松。像控制排气的内部动作；不要用中断尿流练习。</p><p>怎么做：轻轻收紧后完全放松，选择动作感受。点确认会记录此项并进入耐力保持；不确定或代偿会安全停止。</p>${select('动作控制','controlQuality',[['GOOD','较清楚识别'],['UNCERTAIN','不确定'],['COMPENSATED','明显依赖其他肌群']],assessment?.control||'GOOD')}</div>${btn('INT-03','确认动作，进入耐力保持')}${btn('INT-64','跳过摸底，回到今日','',{secondary:true})}${safetyButton('INT-41')}<p class="hint">自我记录，不是临床肌力检查。</p>`;
 if(sc==='SCR-03'){
  if(!assessment)newAssessment();const n=Math.max(1,step);let out=assessmentProgress(n)+title('可选摸底',['','耐力保持（秒）','耐力次数','快速次数','放松质量'][n],['','在保持中感觉明显衰减就停止；不为成绩硬撑。','收紧→完全放松算一次；每做一次点一次“完成一次”。','快速收紧→立刻完全放松；每做一次点一次“完成一次”。','确认能否真正完全放松，异常优先停止。'][n]);
  if(latestAssessmentRecord)out+=`<div class="recent-record" role="status">最近记录：${escapeHTML(latestAssessmentRecord)}</div>`;
  if(n===1)out+=`<p>点“开始保持计时”后秒数增长，感觉衰减点停止。确认有效秒数并点“确认保持结果”会进入耐力次数。</p><div class="test-clock"><strong id="holdDisplay">${holdValue.toFixed(1)}</strong><span>秒，最多10秒</span></div>${holdRunning?btn('INT-04','感觉衰减，停止','stop'):btn('INT-04','开始保持计时','start')}${field('确认有效保持（自报秒数）','holdSeconds',holdValue?Math.floor(holdValue):assessment.seconds??'','number','min="1" max="10" step="1"')}${btn('INT-04','确认保持结果','confirm',{disabled:holdRunning})}`;
  else if(n===2||n===3){const id=n===2?'INT-05':'INT-06',count=n===2?assessment.endurance:assessment.fast;out+=`<div class="test-clock count-feedback"><strong>${count}/10</strong><span>完整收紧与完全放松周期</span></div><p>${n===2?`一次＝按你刚测的${assessment.seconds}秒收紧并保持→完全放松。两次之间完全放松＋休息约5秒（产品操作标准）。次数是在数重复能力，不是再测一次极限；不用脑内数。`:'每次快速收紧后立刻完全放松。'}做完一次再点按钮；一次点击即确认已完成完整周期。点后数字加1，最多10次。测到10次或感觉够了就“结束本项”进入下一项。</p>${btn(id,'完成一次','rep',{disabled:count>=10})}${btn(id,'结束本项','finish',{secondary:true})}`;}
  else out+=`<p>选择本次放松感受；点保存会计算训练等级建议，若不确定或难放松则安全停止。</p>`+select('放松质量','relaxationQuality',[['GOOD','良好，能完全放松'],['UNCERTAIN','不确定'],['POOR','较差，难以放松']],assessment.relaxation)+btn('INT-07','保存本次摸底，查看建议');
  return out+btn('INT-08','上一项','back',{secondary:true})+btn('INT-65','跳过 / 取消摸底，回今日','',{secondary:true})+safetyButton('INT-42');
 }
 if(sc==='SCR-04'){
  const a=assessment||db.assessments.at(-1);let out=title('训练等级建议',st==='ST-46'?'先熟悉动作，再开始':result?.level?`建议从 L${result.level} 开始`:'查看本次建议','这是训练等级建议，不是临床诊断。');if(a)out+=`<div class="card">${row('动作控制',a.control==='GOOD'?'可识别':'需确认')}${row('有效保持',a.seconds+'秒')}${row('耐力重复',a.endurance+'次')}${row('快速收缩',a.fast+'次')}${row('完全放松',a.relaxation==='GOOD'?'良好':'需确认')}</div>`;
  if(result?.level&&st!=='ST-46'){const m=POLICY.six_level_parameters_and_test_cadence.training_matrix[result.level-1];out+=parameters(m)+`<p class="hint">摸底仅用于了解。参考结果已保存，下次打开仍可查看默认建议；当前选择自由，不受此结果限制。</p>${btn('INT-10','用本次结果作为当前选择')}`;}else out+='<p>本次未得到有效参考等级；当前选择不受影响，可回今日自由选L1–L6。持续困难可考虑专业评估。</p>';
  return out+btn('INT-11','暂不采用，返回能力','',{secondary:true})+btn('INT-09','重新计算本次建议','',{secondary:true});
 }
 if(sc==='SCR-05')return title('能力','了解自己，选择节奏','摸底可选；结果与当前选择互不锁定。')+levelInfo()+btn('INT-01','回今日自由选择等级')+btn('INT-67','各级标准','',{secondary:true})+(db.level?parameters(POLICY.six_level_parameters_and_test_cadence.training_matrix[db.level-1]):'')+btn('INT-13','耐力训练','ENDURANCE')+btn('INT-13','快速训练','FAST',{secondary:true})+btn('INT-30','可选：重新摸底','',{secondary:true})+btn('INT-46','可选个人自评','',{secondary:true});
 if(sc==='SCR-07'){
  const p=s?.plan||Domain.plan(db)||POLICY.six_level_parameters_and_test_cadence.training_matrix[0];const ph=s?.phase||'E';const label={E:'收紧 · 保持',ER:'放松',F:'紧',FR:'松',GROUP:'组间休息',END:'完成 · 放松'}[ph];const rem=Math.ceil(s?.remaining||0);const e=s?.doneE||0,f=s?.doneF||0;
  const phaseID={E:'INT-16',F:'INT-17',END:'INT-60',ER:'INT-18',FR:'INT-18',GROUP:'INT-18'}[ph];
  return `<div id="phase-event" hidden data-event="phase-deadline">${btn(phaseID,'阶段计时完成')}</div><div class="topline"><span>${ph==='F'||ph==='FR'?'快速训练':'耐力训练'} ${simulatedBackground?'｜锁屏演示':''}</span><span class="pill">L${p.level||db.level||1}</span></div>${trainingHUD(s)}<h1>${label}</h1><p class="muted">${['ER','FR','GROUP','END'].includes(ph)?'正常呼吸，真正完全放松。':'不夹臀腿，不向下用力。'}</p><div class="orb" id="phaseOrb"><span id="phaseCaption">${label}</span><strong id="remaining">${rem}</strong><span>秒</span></div><div class="controls">${btn('INT-19','暂停')}${btn('INT-22','提前结束','',{secondary:true})}</div>${['ER','FR','GROUP'].includes(ph)?btn('INT-32','还没完全放松','RELAX',{secondary:true}):''}${safetyButton('INT-32')}${levelChoice('INT-71')}<div class="small-actions">${btn('INT-20','模拟来电 / 焦点丢失','',{secondary:true})}${btn('INT-34',simulatedBackground?'回到前台':'模拟锁屏','',{secondary:true})}${btn('INT-39',offlineOverlay?'模拟网络恢复':'模拟离线','',{secondary:true})}</div>`;
 }
 if(sc==='SCR-08'){
  if(st==='ST-44')return title('先放松','再给自己10秒','原阶段已暂停，不进入下一次收紧。')+`<div class="test-clock"><strong id="extensionClock">${Math.ceil(extensionRemaining)}</strong><span>秒额外放松</span></div>${btn('INT-55','已经完全放松，继续','OK',{disabled:extensionRemaining>0||extensionFrozen})}${btn('INT-55','仍无法放松','STILL',{danger:true})}${btn('INT-55','安全结束','END',{secondary:true})}`;
  return title('先停一下',st==='ST-15'?'训练已中断':'训练已暂停','倒计时与总进度已冻结；返回不会自动继续。')+trainingHUD(s)+levelChoice('INT-72')+`<div class="card">${row('原阶段',phaseLabel(s?.phase))}${row('剩余',Math.ceil(s?.remaining||0)+'秒')}${row('已引导耐力/快速',(s?.doneE||0)+' / '+(s?.doneF||0))}</div>${externalInterrupt?extra('模拟通话结束 / 焦点返回','focus-return'):''}${btn('INT-21','主动恢复','',{disabled:!s||st==='ST-16'})}${btn('INT-61','结束本次','',{secondary:true})}${safetyButton('INT-44')}`;
 }
 if(sc==='SCR-09')return title('结束确认','现在结束本次？','保留实际已做量，不补齐计划量。')+`<div class="card">${row('耐力已完成',s?.doneE||0)}${row('快速已完成',s?.doneF||0)}<p>未完整周期单列；日额度不退回。</p></div>${btn('INT-23','确认结束','',{danger:true})}${btn('INT-24','取消，保持暂停','',{secondary:true})}`;
 if(sc==='SCR-10'){
  const x=db.active||db.sessions.at(-1);const map={COMPLETED:'本次训练完成',INCOMPLETE_MANUAL:'本次已提前结束',INCOMPLETE_FOCUS:'中断后结束',INCOMPLETE_BUDGET:'已达当日额度'};return title('训练记录',map[x?.status]||'本次已安全停止','完整训练与提前/安全结束分别记录。')+`<div class="grid2"><div class="metric">耐力完成<strong>${x?.doneE||0}次</strong><small>有效保持${(x?.doneE||0)*(x?.plan.hold_seconds||0)}秒（自报）</small></div><div class="metric">快速完成<strong>${x?.doneF||0}次</strong><small>收紧–完全放松</small></div></div>${row('未完整耐力/快速',(x?.partial?.enduranceIncomplete||0)+' / '+(x?.partial?.fastIncomplete||0))}${row('记录状态',st==='ST-23'?'未保存':st==='ST-18'?'正在保存':'已保存')}${check('本次动作与保持质量有效（自报）','qualityValid',x?.valid!==false)}${btn('INT-26','确认本次质量','',{secondary:true})}${btn('INT-25',st==='ST-23'?'重试保存':'确认保存状态','',{secondary:true})}${btn('INT-01','回到今日')}${btn('INT-27','查看训练记录','',{secondary:true})}${safetyButton('INT-45')}`;
 }
 if(sc==='SCR-11'){
  let out=title('长期记录','把进步留在自己的节奏里','引导完成量与自报保持，不是客观肌力测量。')+select('查看范围','historyRange',[['all','全部记录'],['week','最近7天'],['empty','无记录范围（演示）']],historyRange)+btn('INT-28','查看此范围','',{secondary:true});
  if(st==='ST-24')return out+'<div class="card"><h2>还没有记录</h2><p>完成摸底或训练后，这里分别记录耐力与快速。</p></div>';
  const selected=historyRange==='week'?db.sessions.filter(s=>new Date(s.day+'T12:00:00')>=new Date(Date.now()-7*86400000)):db.sessions;const scoped=Domain.stats({...db,sessions:selected});
  out+=`<div class="grid2"><div class="metric">完成训练日<strong>${scoped.days}天</strong></div><div class="metric">完整训练<strong>${scoped.sessions}次</strong></div></div><div class="card"><h2>耐力</h2>${row('引导完成收缩',scoped.endurance+'次')}${row('有效保持累计（自报）',scoped.hold+'秒')}${row('对应活跃时长',scoped.enduranceTime+'秒')}${row('最长有效保持（自报）',scoped.maxHold+'秒')}${row('当前单主等级',level)}</div><div class="card"><h2>快速</h2>${row('引导完成收缩',scoped.fast+'次')}${row('完整组',scoped.fastGroups+'组')}${row('对应活跃时长',scoped.fastTime+'秒')}${row('当前单主等级',level)}</div><h2>等级变化</h2><div class="timeline">${db.levels.length?db.levels.map(x=>row(x.day,(x.from?'L'+x.from:'首次')+' → L'+x.to)).join(''):'<p>尚无已采用等级事件。</p>'}</div><h2>训练明细</h2>${selected.map(x=>`<details class="record"><summary>${x.day}　L${x.plan.level}　${escapeHTML(statusLabel(x.status))}</summary><p>耐力${x.doneE}次，快速${x.doneF}次；未完整耐力${x.partial?.enduranceIncomplete||0}次、快速${x.partial?.fastIncomplete||0}次另列；旧等级快照保持。</p></details>`).join('')||'<p>尚无训练会话。</p>'}<h2>历次摸底</h2>${db.assessments.map(x=>row(x.day,`${x.seconds??'-'}秒 / ${x.endurance}耐力 / ${x.fast}快速　${statusLabel(x.status)}`)).join('')||'<p>尚无摸底。</p>'}`;return out;
 }
 if(sc==='SCR-12')return title('先照顾身体','本次已经停止','不必硬撑，也不必为了完成次数继续。')+`<div class="alert"><p>疼痛、持续不适、明显排尿排便困难或无法放松时，暂停训练，考虑咨询医生或盆底物理治疗专业人员。</p><p>本次已经停止，不能恢复这次中止会话；没有永久选级锁。持续不适时请休息并寻求专业评估，不为了打卡继续。</p></div>${btn('INT-33','确认安全退出')}`;
 if(sc==='SCR-13')return title('按自己的生活安排','提醒与隐私','这是产品提醒设置，不是医学处方。')+select('每日提醒','frequency',[[1,'1次'],[2,'2次（默认）'],[3,'3次（自动降量）']],db.frequency)+btn('INT-38','保存提醒频率')+`<p class="hint">三次每次最多13次，所有训练共用日上限40。按身体感受休息，不必为了打卡增加训练。</p><div class="card"><h2>通知许可</h2><p>以下按钮仅模拟许可，不是Android系统授权。</p>${btn('INT-36','模拟允许通知','ALLOWED',{secondary:true})}${btn('INT-36','模拟拒绝通知','DENIED',{secondary:true})}</div><div class="card"><h2>个人自评</h2><p>敏感自报、本地/默认不上传、卸载可能丢失。仅本人参考，与训练等级无关。</p>${btn('INT-58','查看 / 单独清除自评记录','',{secondary:true})}</div>`;
 if(sc==='SCR-14')return title('规则配置帮助','可以手动选择继续','使用内置已批准六级矩阵，当前错误不会锁住等级。')+select('手动选择训练等级','fallbackLevel',[1,2,3,4,5,6].map(n=>[n,`L${n}`]),db.level||1)+btn('INT-70','按所选等级开始训练')+btn('INT-40','重试规则读取','prepare',{secondary:true})+btn('INT-01','返回今日','',{secondary:true});
 if(sc==='SCR-17')return standards();
 if(sc==='SCR-15')return title('可选个人自评','只给自己留个参考','敏感自报，不用于诊断，也不说明训练疗效。')+`<div class="card"><p>本地保存、默认不上传，卸载可能丢失；可单独清除。与五维、Level、升级、训练参数、日额度完全分离。</p>${db.ehsEnabled?select('本次自评（1–4）','ehsValue',[['','请选择'],[1,'1 / 4'],[2,'2 / 4'],[3,'3 / 4'],[4,'4 / 4']],ehsValue??'')+check('我理解敏感自报与本地隐私说明，仅本人参考','ehsConsent',ehsConsent)+btn('INT-48','保存本次自评')+btn('INT-51','关闭个人自评','',{secondary:true}):btn('INT-47','自愿启用个人自评')}${btn('INT-50','查看本人自评历史','',{secondary:true})}${btn('INT-49','跳过，返回能力','',{secondary:true})}</div>`;
 if(sc==='SCR-16'){
  let out=title('本人自评记录','与训练记录分开','不与Level换算，不推断疗效。');
  if(st==='ST-55')return out+`<div class="alert"><h2>只清除EHS记录？</h2><p>确认后不可恢复；训练、摸底、等级和日额度保持。</p>${btn('INT-53','确认单独清除','',{danger:true})}${btn('INT-54','取消清除','',{secondary:true})}</div>`;
  out+=db.ehs.length?`<div class="card">${db.ehs.map(x=>row(x.day,x.value+' / 4')).join('')}</div>${btn('INT-52','单独清除EHS历史','',{secondary:true})}`:'<div class="card"><h2>没有自评记录</h2><p>跳过不会生成记录；关闭不影响训练。</p></div>';
  return out+btn('INT-57','返回个人自评管理','',{secondary:true});
 }
 return '';
}
function render(){
 const sc=current.screen;document.body.classList.toggle('training',sc==='SCR-07');for(const section of document.querySelectorAll('[data-screen-id].screen'))section.hidden=section.dataset.screenId!==sc;
 const host=document.querySelector(`.screen[data-screen-id="${sc}"]`);host.innerHTML=contents();host.dataset.activeState=current.state;
 const template=$('#state-'+current.state),stateHost=$('#state-feedback');stateHost.replaceChildren(template.content.cloneNode(true));stateHost.dataset.stateId=current.state;
 stateHost.className=stateById[current.state].kind==='error'?'state-feedback alert':'state-feedback';$('#message').textContent=message;$('#message').hidden=!message;
 if(stateById[current.state].kind==='error'){host.insertAdjacentHTML('afterbegin',extra('重试本屏上次操作','retry'));if(inputDraftScreen===current.screen){for(const [id,v] of Object.entries(inputDraft)){const e=host.querySelector('#'+id);if(e){e.value=v.value;if(e.type==='checkbox')e.checked=v.checked;}}}}
 $('#navigation').innerHTML=nav();$('#navigation').hidden=['SCR-07','SCR-08','SCR-09','SCR-12'].includes(sc)||!!db.active;
 $('#review-current').textContent=`${sc} / ${current.state}`;$('#review-preview').hidden=!preview;$('#review-count').textContent=`执行${log.interactions.length}次交互，观察${new Set(log.states).size}个状态（仅本页自报日志）`;
}
function demo(mode){stopVoice();holdRunning=false;externalInterrupt=false;extensionFrozen=false;offlineOverlay=false;preview=false;db=Domain.initial();assessment=null;result=null;planDraft=null;
 if(mode!=='fresh'){db.level=3;db.levelSource='ASSESSMENT';db.everLeveled=true;db.trainingEnabled=true;const p=Domain.plan(db);db.levels=[{day:Domain.date(),from:null,to:3,id:uid()}];if(mode==='six'||mode==='fourteen'){const count=mode==='six'?6:14;for(let n=count;n>0;n--){const d=new Date();d.setDate(d.getDate()-n);db.sessions.push({id:uid(),day:Domain.date(d),plan:{...p},doneE:p.E,doneF:p.F,status:'COMPLETED',valid:true});}}if(mode==='quota')for(let n=0;n<40;n++)db.quota.push({key:'sample-'+n,day:Domain.date()});}
 persist('INT-01');act('INT-01');
}
function review(action,value){
 if(action==='seed'){demo(value);return;}
 if(action==='rules-fault'){rulesFault=true;go('ST-28','模拟规则配置错误：可手动选级继续。');return;}
 if(action==='speed'){speed=Number(value);stopVoice();lastTick=performance.now();$('#speed-note').textContent=speed===1?'真实秒数；仅演示，按身体感受停止。':'20倍流程快进，不可按此节奏跟练；不验证语音。';return;}
 if(action==='caption'){captionOnly=!captionOnly;stopVoice();$('#caption-note').textContent=captionOnly?'仅模拟字幕提示（不验证语音）':'系统语音演示（需设备支持）';return;}
 if(action==='focus-return'){externalInterrupt=false;if(db.active?.extensionPending){extensionFrozen=false;go('ST-44','中断已返回，先完成额外放松。');}else go('ST-15','通话 / 焦点已返回。仍需主动恢复。');return;}
 if(action==='fault'){failNext=true;$('#fault-note').textContent='下一次产品操作将模拟失败；原有效数据保持。';return;}
 if(action==='retry'){if(db.active?.paused&&current.screen==='SCR-07'){go('ST-14','校验已返回；仍保持暂停，请主动恢复。');return;}const prev=[...log.events].reverse().find(e=>e.type==='interaction');if(prev)act(prev.interaction_id,prev.arg);return;}
 if(action==='inspect-state'){stopVoice();if(db.active)db.active.paused=true;preview=true;go($('#inspectState').value,'状态样例预览，不等于该状态已被真实流程或浏览器验证。');return;}
 if(action==='export'){const b=new Blob([JSON.stringify({kind:'CLIENT_SELF_REPORTED_LOG_NOT_QA_PROOF',input_bindings:BINDINGS,log,generated_at:new Date().toISOString()},null,2)],{type:'application/json'});const a=document.createElement('a');a.href=URL.createObjectURL(b);a.download='prototype-client-log.json';a.click();URL.revokeObjectURL(a.href);return;}
}
document.addEventListener('change',e=>{const el=e.target;if(['INT-69','INT-71','INT-72'].includes(el.dataset.interactionId)){try{act(el.dataset.interactionId,el.value);}catch(err){console.error(err);go('ST-22','选择未能保存，请重试。');}}});
document.addEventListener('click',e=>{const b=e.target.closest('button');if(!b||b.disabled)return;try{if(b.dataset.interactionId)act(b.dataset.interactionId,b.dataset.value||'');else if(b.dataset.reviewAction)review(b.dataset.reviewAction,b.dataset.value||'');}catch(err){log.errors.push({kind:'handler',message:String(err)});console.error(err);go('ST-22','操作未完成，请在演示面板重置示例。');}});
window.addEventListener('error',e=>log.errors.push({kind:'page',message:e.message}));window.addEventListener('unhandledrejection',e=>log.errors.push({kind:'promise',message:String(e.reason)}));
document.addEventListener('visibilitychange',()=>{if(document.hidden&&db.active&&!db.active.paused)act('INT-20','浏览器进入后台，媒体能力未验证，保守暂停');if(document.hidden&&db.active?.extensionPending){extensionFrozen=true;persist('INT-55');}});
window.addEventListener('offline',()=>{if(db.active&&!db.active.paused)act('INT-39');});
setInterval(()=>{const now=performance.now(),delta=Math.min(1,(now-lastTick)/1000);lastTick=now;
 if(holdRunning){holdValue=Math.min(10,(now-holdStart)/1000);if($('#holdDisplay'))$('#holdDisplay').textContent=holdValue.toFixed(1);if(holdValue>=10){holdRunning=false;holdStart=null;render();}}
 if(db.active?.extensionPending&&current.state==='ST-44'&&!extensionFrozen){const old=extensionRemaining;extensionRemaining=Math.max(0,extensionRemaining-delta*speed);db.active.extensionRemaining=extensionRemaining;if(Math.ceil(old)!==Math.ceil(extensionRemaining)&&!persist('INT-55')){extensionFrozen=true;return;}if($('#extensionClock'))$('#extensionClock').textContent=Math.ceil(extensionRemaining);if(old>0&&extensionRemaining===0)render();}
 const s=db.active;if(s&&!s.paused&&current.screen==='SCR-07'){const used=Math.min(s.remaining,delta*speed);s.elapsedLogical=(s.elapsedLogical||0)+used;s.remaining=Math.max(0,s.remaining-used);const pct=Math.min(100,Math.floor(100*s.elapsedLogical/s.plan.duration));if($('#totalPercent'))$('#totalPercent').textContent=pct+'%';if($('#totalBar'))$('#totalBar').style.width=pct+'%';if($('#remaining'))$('#remaining').textContent=Math.ceil(s.remaining);if(s.remaining===0)advance();}
},100);
try{const raw=localStorage.getItem(KEY);if(raw)db={...Domain.initial(),...JSON.parse(raw)};db.safetyLock=false;db.lowerPending=false;db.trainingEnabled=true;if(!db.lastAssessment){const last=[...db.assessments].reverse().find(x=>['COMPLETE','APPLIED','NOT_APPLIED'].includes(x.status)&&x.seconds!==null);if(last){const r=Domain.candidate(last);db.lastAssessment={id:last.id,level:r.level,reason:r.reason,day:last.day};}}if(!db.level&&db.lastAssessment?.level){db.level=db.lastAssessment.level;db.levelSource='ASSESSMENT_DEFAULT';}if(db.active){if(db.active.phase==='PREP'){const first=db.active.plan.E>0?'E':'F';db.active.phase=first;db.active.remaining=first==='E'?db.active.plan.hold_seconds:db.active.plan.fast_seconds;db.active.plan.duration=Math.max(0,db.active.plan.duration-10);Domain.quota(db,`${db.active.id}:${first}:0`);}db.active.paused=true;externalInterrupt=false;if(db.active.extensionPending){extensionRemaining=db.active.extensionRemaining??10;extensionFrozen=true;}}}catch(e){storageOK=false;log.errors.push({kind:'storage',message:String(e)});}
window.PrototypeSimulator={getLog:()=>structuredClone(log),getSnapshot:()=>structuredClone({db,current,speed,captionOnly,preview}),bindings:BINDINGS};
$('#inspectState').innerHTML=CONTRACT.states.map(s=>`<option value="${s.state_id}">${s.screen_id} / ${s.state_id} ${s.semantic}</option>`).join('');
observed();render();if(storageOK)act('INT-01');else go('ST-22','本地演示空间无法读取；不能假报成功。');
