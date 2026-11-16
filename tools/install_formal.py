from pathlib import Path
import json,zipfile,io,hashlib,os,sys,uuid,re,psutil
sys.stdout.reconfigure(encoding='utf-8');sys.stderr.reconfigure(encoding='utf-8')
S=Path(__file__).resolve().parent.parent;D=S.parent/'正式交付/0.2.42';V='0.2.42'
OWN={'fabric_train_simulator','station_access'}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def meta(p):
 with zipfile.ZipFile(p) as z:return json.loads(z.read('fabric.mod.json')) if 'fabric.mod.json' in z.namelist() else {}
def vers(v):return tuple(int(n) for n in v.split('+')[0].split('.') if n.isdigit())
def no_games(games):
 wanted={str(Path(x)).replace('\\','/').lower() for x in games}
 for p in psutil.process_iter(['pid','name']):
  if p.info['name'].lower() not in ['java.exe','javaw.exe']:continue
  try:args=p.cmdline()
  except psutil.NoSuchProcess:continue
  except psutil.AccessDenied:raise RuntimeError('无法核对Java进程：'+str(p.pid))
  expanded=list(args)
  for arg in args:
   if arg.startswith('@') and Path(arg[1:]).is_file():
    raw=Path(arg[1:]).read_bytes()
    try:t=raw.decode('utf-8-sig')
    except UnicodeDecodeError:t=raw.decode('cp936')
    for line in t.splitlines():
     try:x=json.loads(line)
     except ValueError:continue
     if isinstance(x,str):expanded.append(x)
  for i,a in enumerate(expanded):
   if a=='--gameDir' and i+1<len(expanded) and str(Path(expanded[i+1])).replace('\\','/').lower() in wanted:raise RuntimeError('目标游戏正在运行，需先正常保存退出。进程：'+str(p.pid))
def check(mc,t,jar):
 game=Path(t['game']);profile=json.loads(Path(t['profile']).read_text(encoding='utf-8-sig'))
 with zipfile.ZipFile(game/(profile['id']+'.jar')) as z:actual=json.loads(z.read('version.json'))['id']
 assert actual==mc,(game,actual,mc)
 loaders=[x['name'].rsplit(':',1)[1] for x in profile['libraries'] if x['name'].startswith('net.fabricmc:fabric-loader:')];assert len(loaders)==1
 found=[]
 for p in (game/'mods').glob('fabric-api-*.jar'):
  if meta(p).get('id')=='fabric-api':found.append(p)
 assert len(found)==1,(game,'API未唯一确定')
 api=found[0];available={'fabricloader':loaders[0]}
 with zipfile.ZipFile(api) as z:
  m=json.loads(z.read('fabric.mod.json'));available[m['id']]=m['version']
  for part in m.get('jars',[]):
   with zipfile.ZipFile(io.BytesIO(z.read(part['file']))) as nested:
    n=json.loads(nested.read('fabric.mod.json'));available[n['id']]=n['version']
 required=meta(jar)['depends']
 for name,condition in required.items():
  if name in ['minecraft','java']:continue
  if isinstance(condition,str) and condition.startswith('>='):
   assert name in available and vers(available[name])>=vers(condition[2:]),(game,name,available.get(name),condition)
 return {'minecraft':mc,'game':str(game),'profile':str(t['profile']),'loader':loaders[0],'fabric_api':available['fabric-api']}
def main():
 targets=json.loads((S/'docs/INTEGRATED_INSTALL_TARGETS.json').read_text(encoding='utf-8'));reports=json.loads((D/'正式验证汇总.json').read_text(encoding='utf-8'))
 pairs=[(t['minecraft'],g) for t in targets for g in [t]+t.get('additional_games',[])]
 no_games([t['game'] for _,t in pairs]);prepared=[]
 for mc,t in pairs:
  jar=D/('mc'+mc)/f'fabric-train-simulator-{V}-mc{mc}-fabric.jar';assert sha(jar)==reports[mc]['runtime_sha256'];prepared.append((jar,check(mc,t,jar)))
 installed=[]
 for jar,record in prepared:
  mods=(Path(record['game'])/'mods').resolve();dest=mods/jar.name;stage=mods/(jar.name+'.stage-'+uuid.uuid4().hex);moved=[]
  with stage.open('xb') as f:f.write(jar.read_bytes());f.flush();os.fsync(f.fileno())
  assert sha(stage)==sha(jar)
  try:
   no_games([record['game']])
   for old in mods.glob('*.jar'):
    try:identity=meta(old).get('id')
    except FileNotFoundError:continue
    if identity not in OWN:continue
    if old==dest and sha(old)==sha(jar):continue
    backup=old.with_name(old.name+'.before-formal-'+uuid.uuid4().hex);old.rename(backup);moved.append((old,backup))
   os.replace(stage,dest);assert sha(dest)==sha(jar) and meta(dest)['version']==V
   active=[p for p in mods.glob('*.jar') if meta(p).get('id') in OWN];assert active==[dest],active
  except BaseException:
   if dest.exists() and sha(dest)==sha(jar):dest.rename(dest.with_name(dest.name+'.failed-formal-'+uuid.uuid4().hex))
   for old,bak in moved:
    if not old.exists():bak.rename(old)
   raise
  finally:
   if stage.exists():stage.unlink()
  installed.append({**record,'runtime':str(dest),'sha256':sha(dest),'old_renamed':[str(b) for _,b in moved]})
  (D/'安装进度.json').write_text(json.dumps(installed,ensure_ascii=False,indent=2),encoding='utf-8');print('正式安装',record['game'],flush=True)
 (D/'安装记录.json').write_text(json.dumps(installed,ensure_ascii=False,indent=2),encoding='utf-8')
 (S/'docs/正式安装记录.json').write_text(json.dumps(installed,ensure_ascii=False,indent=2),encoding='utf-8')
if __name__=='__main__':main()
