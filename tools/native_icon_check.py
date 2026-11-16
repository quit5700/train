from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import os,sys,json,subprocess,zipfile,hashlib,ctypes,shutil,argparse
sys.stdout.reconfigure(encoding='utf-8');sys.stderr.reconfigure(encoding='utf-8')
S=Path(__file__).resolve().parent.parent;P=S.parent;V='0.2.42';D=P/'正式交付'/V
MCS=['1.20.1','1.20.4','1.21.1','1.21.4','1.21.8','1.21.10','1.21.11','26.1.2','26.2','26.3']
C=Path('H:/CodexWork/q5-gradle-cache/train-formal-temp/icon-clients');C.mkdir(parents=True,exist_ok=True)
UI_WORLD=None
targets=json.loads((S/'docs/INTEGRATED_INSTALL_TARGETS.json').read_text(encoding='utf-8'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def argfile(path,args,enc='utf-8'):
 path.write_text('\n'.join(json.dumps(str(a).replace('\\','/'),ensure_ascii=False) for a in args),encoding=enc);return '@'+str(path)
def prepare(mc):
 base=S/'versions'/V/('mc'+mc);jar=base/'dist'/f'fabric-train-simulator-{V}-mc{mc}-fabric.jar';target=next(t for t in targets if t['minecraft']==mc)
 case=C/('mc'+mc);i=1
 while case.exists():i+=1;case=C/('mc'+mc+'-try'+str(i))
 case.mkdir();mods=case/'mods';mods.mkdir();shutil.copy2(jar,mods/jar.name);api=Path(target['api_file']);shutil.copy2(api,mods/api.name)
 if UI_WORLD is not None:
  seen={'fabric_train_simulator','station_access','fabric-api'}
  for dependency in (Path(target['game'])/'mods').glob('*.jar'):
   with zipfile.ZipFile(dependency) as z:
    if 'fabric.mod.json' not in z.namelist():continue
    identity=json.loads(z.read('fabric.mod.json'))['id']
   if identity in seen:continue
   seen.add(identity);shutil.copy2(dependency,mods/dependency.name)

 props=dict(line.split('=',1) for line in (base/'gradle.properties').read_text(encoding='utf-8').splitlines() if '=' in line and not line.lstrip().startswith('#'));jdk=Path(props['org.gradle.java.home'])
 env={**os.environ,'GRADLE_USER_HOME':'H:/CodexWork/q5-gradle-cache','JAVA_TOOL_OPTIONS':'-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Djava.io.tmpdir=H:/CodexWork/q5-gradle-cache/train-formal-temp'}
 with (case/'classpath.log').open('wb') as f:c=subprocess.run(['cmd','/d','/c','gradlew.bat','writeReleaseProbeClasspath','--offline','--no-daemon','--no-watch-fs','--console=plain','--stacktrace'],cwd=base,env=env,stdout=f,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW).returncode
 assert c==0,(mc,'当前SDK类路径生成失败',case)
 cp=(base/'build/release-probe-classpath.txt').read_text(encoding='utf-8');classes=case/'classes';classes.mkdir();release='17' if mc.startswith('1.20.') else '25' if mc.startswith('26.') else '21'
 source=S/'tools/diagnostics/FormalIconProbe.java'
 if UI_WORLD is not None:
  source=case/'UiPauseProbe.java';text=(S/'tools/diagnostics/UiPauseProbe.java').read_text(encoding='utf-8')
  if mc.startswith('26.'):text=text.replace('mc.setScreen(', 'mc.setScreenAndShow(').replace('static final int RIGHT_BUTTON=1;', 'static final int RIGHT_BUTTON=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;')
  source.write_text(text,encoding='utf-8',newline='\n')
 result=subprocess.run([str(jdk/'bin/javac.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8',argfile(case/'javac.args',['--release',release,'-encoding','UTF-8','-proc:none','-cp',cp,'-d',classes,source])],capture_output=True,env=env,creationflags=subprocess.CREATE_NO_WINDOW)
 raw=result.stdout+result.stderr
 try: compiler_log=raw.decode('utf-8')
 except UnicodeDecodeError: compiler_log=raw.decode('cp936')
 (case/'编译日志.txt').write_text(compiler_log,encoding='utf-8');assert result.returncode==0,(mc,compiler_log)
 with zipfile.ZipFile(mods/'formal-icon-probe.jar','w',zipfile.ZIP_DEFLATED) as z:
  for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes).as_posix())
  z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'train_release_icon_probe','version':'0.0.0','entrypoints':{e:['cn.quit5700.qadiagnostic.UiPauseProbe' if UI_WORLD is not None else 'cn.quit5700.trainrelease.FormalIconProbe'] for e in ['main','client']}}))
 from production_launch import prepare as launch
 command=launch(mc,target,case,base,str(jdk/'bin/java.exe'))
 command.insert(1,'-Dtrain.release.result='+str(case/'result.json'));command.insert(1,'-Dq5.full.result='+str(case/'result.json'));command.extend(['--gameDir',str(case),'--width','840','--height','600','--username','quit5700','--uuid','b581e3101c2b47e390f5d869fec34d97'])
 if UI_WORLD is not None:
  assert (UI_WORLD/'level.dat').is_file(),'必须指定同游戏版本已退出的测试存档'
  world=case/'saves/界面回归';world.parent.mkdir();shutil.copytree(UI_WORLD,world,ignore=shutil.ignore_patterns('session.lock'));command.extend(['--quickPlaySingleplayer','界面回归'])
 (case/'options.txt').write_text('lang:zh_cn\nguiScale:2\nrenderDistance:2\nsimulationDistance:2\npauseOnLostFocus:false\nonboardAccessibility:false\nmaxFps:30\n',encoding='utf-8')
 record={'minecraft':mc,'case':str(case),'runtime_sha256':sha(jar),'snapshot_sha256':sha(mods/jar.name),'command':command}
 (case/'启动准备.json').write_text(json.dumps({k:v for k,v in record.items() if k!='command'},ensure_ascii=False,indent=2),encoding='utf-8')
 print('已准备',mc,flush=True);return record
def run(r):
 case=Path(r['case']);command=r['command'];argument=argfile(case/'java.args',command[1:],'cp'+str(ctypes.windll.kernel32.GetACP()))
 with (case/'客户端.log').open('wb') as f:
  p=subprocess.Popen([command[0],argument],cwd=case,stdout=f,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
  (case/'进程.json').write_text(json.dumps({'pid':p.pid,'minecraft':r['minecraft']},ensure_ascii=False),encoding='utf-8')
  try:code=p.wait(timeout=360)
  except subprocess.TimeoutExpired:p.kill();p.wait();raise
 result=json.loads((case/'result.json').read_text(encoding='utf-8')) if (case/'result.json').exists() else {}
 report={**{k:v for k,v in r.items() if k!='command'},'returncode':code,'result':result}
 dest=D/'验证记录'/('mc'+r['minecraft'])/('原生界面回归' if UI_WORLD is not None else '正式原生图标');dest.mkdir(parents=True,exist_ok=True)
 for name in ['result.json','客户端.log','编译日志.txt','classpath.log','诊断包生产重映射.log']:
  if (case/name).exists():shutil.copy2(case/name,dest/name)
 (dest/'启动核验.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
 assert code==0 and result.get('passed') and all(v is True for v in result['checks'].values()),(r['minecraft'],code,result)
 print('正式原生图标通过',r['minecraft'],flush=True);return report
if __name__=='__main__':
 parser=argparse.ArgumentParser(description='模拟火车原生正式图标／界面回归；所有世界操作只在临时副本内')
 parser.add_argument('--mc',nargs='+',default=MCS);parser.add_argument('--ui',action='store_true');parser.add_argument('--world-copy',type=Path)
 args=parser.parse_args();selected=args.mc
 if args.ui:
  assert len(selected)==1 and args.world_copy,'界面回归必须指定一个游戏版本及同版已退出的测试存档'
  UI_WORLD=args.world_copy
 ready=[prepare(mc) for mc in selected] # 共享映射缓存的准备阶段必须串行。
 with ThreadPoolExecutor(max_workers=1) as pool:results=list(pool.map(run,ready))
 (D/('正式原生验证-'+','.join(selected)+'.json')).write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
