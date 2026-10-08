/* 纯产品规则：构建时嵌入单一 HTML；规则值由正式 UX 注入。 */
const Domain = (() => {
  const date = (v=new Date()) => {const d=new Date(v);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;};
  const initial = () => ({version:4,lastAssessment:null,levelSource:null,level:null,everLeveled:false,trainingEnabled:false,lowerPending:false,safetyLock:false,frequency:2,assessments:[],levels:[],sessions:[],quota:[],ehsEnabled:false,ehs:[],promptShown:false,restChoices:{},active:null});
  const selectStartingLevel = (db,level,ruleVersion="UX-V1.4") => {
    if(!Number.isInteger(level)||level<1||level>6)return false;
    const from=db.level;db.level=level;db.levelSource='USER_SELECTED';db.trainingEnabled=true;db.everLeveled=true;db.safetyLock=false;db.lowerPending=false;
    if(from!==level)db.levels.push({day:date(),from,to:level,source:'USER_SELECTED',createdAt:new Date().toISOString(),ruleVersion,id:'selection-'+Date.now()});return true;
  };
  const candidate = (a) => {
    if(a.control!=='GOOD')return {level:null,reason:'NO_LEVEL_CONTROL'};
    if(a.relaxation!=='GOOD')return {level:null,reason:'NO_LEVEL_RELAX'};
    if(a.symptom)return {level:null,reason:a.symptom==='SYMPTOM'?'NO_LEVEL_SYMPTOM':'NO_LEVEL_PAIN'};
    const row=[...POLICY.initial_level_decision_table.thresholds].reverse().find(t=>a.seconds>=t.endurance_seconds_min&&a.endurance>=t.endurance_reps_min&&a.fast>=t.fast_reps_min);
    return {level:row?Number(row.level.slice(1)):null,reason:row?'SAFE':'NO_LEVEL'};
  };
  const recommend = (db,a) => candidate(a);
  const used = (db,day=date()) => db.quota.filter(x=>x.day===day).length;
  const quota = (db,key,day=date()) => {if(db.quota.some(x=>x.key===key))return true;if(used(db,day)>=40)return false;db.quota.push({key,day});return true;};
  const matrixValid = rows => Array.isArray(rows)&&rows.length===6&&rows.every((r,i)=>r&&r.level==='L'+(i+1)&&['hold_seconds','rest_seconds','endurance_reps','fast_seconds','fast_rest_seconds','fast_reps'].every(k=>Number.isInteger(r[k])&&r[k]>0&&r[k]<=10));
  const plan = (db,mode='COMBINED',half=false,matrix=POLICY.six_level_parameters_and_test_cadence.training_matrix) => {
    if(!Number.isInteger(db.level)||db.level<1||db.level>6)return null;
    if(!matrixValid(matrix))return null;const m={...matrix[db.level-1]};let E=m.endurance_reps,F=m.fast_reps;
    if(db.frequency===3){E=Math.min(E,6);F=Math.min(F,13-E);}
    if(half){E=Math.max(1,Math.floor(E/2));F=Math.max(1,Math.floor(F/2));}
    const R=Math.max(0,40-used(db));
    if(mode==='COMBINED'){if(R<2)return null;E=Math.min(E,R-1);F=Math.min(F,R-E);}
    if(mode==='ENDURANCE'){E=Math.min(E,R);F=0;}
    if(mode==='FAST'){F=Math.min(F,R);E=0;}
    if(E+F===0)return null;
    return {...m,E,F,mode,half,frequency:db.frequency,level:db.level,duration:E*(m.hold_seconds+m.rest_seconds)+F*(m.fast_seconds+m.fast_rest_seconds)+10+(mode==='COMBINED'?10:0)};
  };
  const completedDays = db => [...new Set(db.sessions.filter(x=>x.status==='COMPLETED').map(x=>x.day))].sort();
  const streak = (db,day=date()) => {const days=new Set(completedDays(db));let count=0;const d=new Date(day+'T12:00:00');d.setDate(d.getDate()-1);while(days.has(date(d))&&db.restChoices[date(d)]!=='REST_DAY'){count++;d.setDate(d.getDate()-1);}return count;};
  const stats = db => {
    const complete=db.sessions.filter(s=>s.status==='COMPLETED');
    return {days:new Set(complete.map(s=>s.day)).size,sessions:complete.length,endurance:db.sessions.reduce((n,s)=>n+s.doneE,0),fast:db.sessions.reduce((n,s)=>n+s.doneF,0),hold:db.sessions.reduce((n,s)=>n+(s.valid===false?0:s.doneE*s.plan.hold_seconds),0),enduranceTime:db.sessions.reduce((n,s)=>n+s.doneE*(s.plan.hold_seconds+s.plan.rest_seconds),0),fastTime:db.sessions.reduce((n,s)=>n+s.doneF*(s.plan.fast_seconds+s.plan.fast_rest_seconds),0),fastGroups:db.sessions.filter(s=>s.plan.F>0&&s.doneF===s.plan.F).length,maxHold:Math.max(0,...db.sessions.filter(s=>s.doneE>0&&s.valid!==false).map(s=>s.plan.hold_seconds))};
  };
  return {date,initial,selectStartingLevel,candidate,recommend,used,quota,matrixValid,plan,completedDays,streak,stats};
})();
