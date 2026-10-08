const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..');
const ux=JSON.parse(fs.readFileSync(path.join(root,'UX_CONTRACT.json'),'utf8'));
const POLICY=Object.fromEntries(Object.entries(ux.required_product_policy).map(([k,v])=>[k,v.approved_value]));
const context={POLICY,Date};vm.createContext(context);vm.runInContext(fs.readFileSync(path.join(__dirname,'domain.js'),'utf8')+';globalThis.D=Domain;',context);const D=context.D;let checks=0;
const ok=(x,msg)=>{assert.ok(x,msg);checks++;};
const a=(s,e,f)=>({seconds:s,endurance:e,fast:f,control:'GOOD',relaxation:'GOOD'});
for(const [s,e,f,want] of [[2,10,10,null],[3,4,10,null],[3,5,4,null],[3,5,5,1],[4,6,6,2],[5,8,8,3],[6,8,10,4],[8,10,10,5],[10,10,10,6]])ok(D.candidate(a(s,e,f)).level===want,'短板边界');
for(const x of [{control:'UNCERTAIN'},{control:'COMPENSATED'},{relaxation:'UNCERTAIN'},{relaxation:'POOR'},{symptom:'PAIN'},{symptom:'SYMPTOM'}])ok(D.candidate({...a(10,10,10),...x}).level===null,'安全门');
let db=D.initial();db.level=3;db.everLeveled=true;db.trainingEnabled=true;ok(D.recommend(db,a(8,10,10)).level===5,'摸底参考不按当前级封顶');ok(D.recommend(db,a(4,6,6)).level===2,'降低建议');db.level=null;ok(D.recommend(db,a(10,10,10)).level===6,'无当前级不限制参考结果');db.everLeveled=false;ok(D.recommend(db,a(10,10,10)).level===6,'首次最高满足档');
for(let level=1;level<=6;level++){db=D.initial();db.level=level;db.trainingEnabled=true;let p=D.plan(db);ok(p.duration===[90,104,132,146,210,250][level-1],'六级标准时长');db.frequency=3;p=D.plan(db);ok((p.E+p.F)*3<=40,'三次不超过40');if(level===6){ok(p.E===6&&p.F===7&&p.duration===161,'L6三次批准例');const h=D.plan(db,'COMBINED',true);ok(h.E===3&&h.F===3,'频率后半量');}}
for(let used=0;used<=40;used++){db=D.initial();db.level=6;db.trainingEnabled=true;for(let n=0;n<used;n++)D.quota(db,'q'+n);const p=D.plan(db);ok(used<39?!!p&&p.E>=1&&p.F>=1&&used+p.E+p.F<=40:p===null,'日剩余组合边界');if(used===39){const q=D.plan(db,'FAST');ok(q.F===1&&q.E===0,'只剩1次可做专项');}}
db=D.initial();ok(D.quota(db,'same'),'首次占用');ok(D.quota(db,'same')&&D.used(db)===1,'恢复同周期不重复扣');db.sessions.push({id:'abort',day:D.date(),status:'SAFETY_ABORT_RELAX',doneE:0,doneF:0,plan:{hold_seconds:3,rest_seconds:6,fast_seconds:1,fast_rest_seconds:4,F:5}});ok(D.used(db)===1&&D.stats(db).sessions===0,'中止不退款、不计完整');ok(D.used(db,'2000-01-01')===0,'其他日期独立');
for(const flag of ['safetyLock','lowerPending']){db=D.initial();db.level=3;db.trainingEnabled=true;db[flag]=true;ok(D.plan(db)!==null,'旧锁不阻断正常选择与训练');}
db=D.initial();db.level=3;db.trainingEnabled=true;const p=D.plan(db);for(let n=1;n<=14;n++){let d=new Date();d.setDate(d.getDate()-n);db.sessions.push({id:String(n),day:D.date(d),status:'COMPLETED',doneE:p.E,doneF:p.F,plan:p});}ok(D.completedDays(db).length===14&&D.streak(db)===14,'14不同日及连续日');db.sessions.push({...db.sessions[0],id:'duplicate-date'});ok(D.completedDays(db).length===14,'同日多次不加天数');db.restChoices[db.sessions[0].day]='REST_DAY';ok(D.streak(db)===0,'休息重置连续日');
const before=JSON.stringify({level:db.level,quota:db.quota,active:db.active,sessions:db.sessions});db.ehsEnabled=true;db.ehs.push({day:D.date(),value:4});db.ehs=[];ok(before===JSON.stringify({level:db.level,quota:db.quota,active:db.active,sessions:db.sessions}),'EHS独立');
// 对生成的真实内联脚本作语法检查；这里没有浏览器或DOM执行。
const html=fs.readFileSync(path.join(root,'prototype.html'),'utf8');const script=html.match(/<script>([\s\S]*?)<\/script>/)[1];new vm.Script(script);checks++;
const report={mode:'STATIC_NODE_RULE_CHECKS_NOT_BROWSER_RUNTIME',status:'PASS',checks,browser_executed:false,console_inspected:false,limitations:'规则函数与内联脚本语法检查；没有实际页面点击、console检查或截图，不能支持Prototype PASS。'};
fs.writeFileSync(path.join(root,'static-rule-checks.json'),JSON.stringify(report,null,2)+'\n');console.log(report);
