import sys
sys.stdout.reconfigure(encoding='utf-8');sys.stderr.reconfigure(encoding='utf-8')
from pathlib import Path
import json,os,re,zipfile,subprocess,sys

def prepare(mc,row,case,base,java):
 game=Path(row['game']);profile=json.loads(Path(row['profile']).read_text(encoding='utf-8-sig'));home=game.parent.parent;natives=case/'natives';natives.mkdir(exist_ok=True)
 def allowed(item):
  if 'rules' not in item:return True
  result=False
  for rule in item['rules']:
   osrule=rule.get('os',{});match=osrule.get('name','windows')=='windows' and osrule.get('arch','amd64') not in ['x86','aarch64','arm64']
   if 'version' in osrule:match=match and bool(re.search(osrule['version'],'10.0'))
   if any(rule.get('features',{}).values()):match=False
   if match:result=rule['action']=='allow'
  return result
 def artifact(name):
  bits=name.split(':');group,lib,v=bits[:3];suffix='-'+bits[3] if len(bits)>3 else ''
  return home/'libraries'/group.replace('.','/')/lib/v/(lib+'-'+v+suffix+'.jar')
 libs=[]
 for lib in profile['libraries']:
  if not allowed(lib):continue
  p=home/'libraries'/lib['downloads']['artifact']['path'] if 'artifact' in lib.get('downloads',{}) else artifact(lib['name']);assert p.exists(),str(p);libs.append(p)
  classifier=lib.get('natives',{}).get('windows','').replace('${arch}','64')
  native=lib.get('downloads',{}).get('classifiers',{}).get(classifier)
  if native:libs.append(home/'libraries'/native['path'])
 for p in libs:
  if 'natives-windows' in p.name:
   with zipfile.ZipFile(p) as z:
    for name in z.namelist():
     if name.lower().endswith('.dll'):(natives/Path(name).name).write_bytes(z.read(name))
 libs.append(game/(profile['id']+'.jar'));assert all(p.exists() for p in libs)
 if not mc.startswith('26.'):
  init=case/'remap-probe.init.gradle';output=case/'production-probe';output.mkdir(exist_ok=True)
  init.write_text("gradle.projectsEvaluated { rootProject.tasks.named('remapJar') { inputFile.set(rootProject.file(System.getProperty('train.probe.input'))); destinationDirectory.set(rootProject.file(System.getProperty('train.probe.output'))); archiveFileName.set('q5-production-probe.jar') } }\n",encoding='utf-8')
  command=['cmd','/d','/c','gradlew.bat','remapJar','--offline','--no-daemon','--no-watch-fs','--console=plain','--init-script',str(init),'-Dtrain.probe.input='+str(case/'mods/formal-icon-probe.jar'),'-Dtrain.probe.output='+str(output),'--stacktrace']
  env={**os.environ,'GRADLE_USER_HOME':'H:/CodexWork/q5-gradle-cache','JAVA_TOOL_OPTIONS':'-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Djava.io.tmpdir=H:/CodexWork/q5-gradle-cache/train-formal-temp'}
  with (case/'诊断包生产重映射.log').open('wb') as log:r=subprocess.run(command,cwd=Path(__file__).parent.parent/'versions/0.2.42'/('mc'+mc),env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW).returncode
  assert r==0,'production probe remap failed'
  target=output/'q5-production-probe.jar';assert target.exists();(case/'mods/formal-icon-probe.jar').write_bytes(target.read_bytes())
 return [java,'-Xmx3G','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-Djava.io.tmpdir=H:/CodexWork/q5-gradle-cache/train-formal-temp','-Djava.library.path='+str(natives),'-Dorg.lwjgl.system.SharedLibraryExtractPath='+str(natives),'-Djna.tmpdir='+str(natives),'-Dminecraft.launcher.brand=TrainQA','-Dminecraft.launcher.version=test42','-cp',';'.join(str(p) for p in dict.fromkeys(libs)),profile['mainClass'],'--version',profile['id'],'--assetsDir',str(home/'assets'),'--assetIndex',profile['assetIndex']['id'],'--accessToken','0','--userType','legacy']
