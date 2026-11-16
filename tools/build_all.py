from pathlib import Path
import argparse,os,subprocess,sys
sys.stdout.reconfigure(encoding='utf-8');sys.stderr.reconfigure(encoding='utf-8')
SOURCE=Path(__file__).resolve().parent.parent
VERSIONS=['1.20.1','1.20.4','1.21.1','1.21.4','1.21.8','1.21.10','1.21.11','26.1.2','26.2','26.3']
def main():
 p=argparse.ArgumentParser(description='模拟火车串行构建，保留每版已有接口和回归测试')
 p.add_argument('--mc',nargs='+',default=VERSIONS);p.add_argument('--version',default='0.2.42');p.add_argument('--offline',action='store_true');args=p.parse_args()
 cache=Path(os.environ.get('GRADLE_USER_HOME','H:/CodexWork/q5-gradle-cache'));temp=cache/'train-formal-temp';temp.mkdir(parents=True,exist_ok=True)
 env={**os.environ,'GRADLE_USER_HOME':str(cache),'JAVA_TOOL_OPTIONS':'-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Djava.io.tmpdir='+str(temp)}
 for mc in args.mc:
  base=SOURCE/'versions'/args.version/('mc'+mc)
  if not (base/'gradlew.bat').is_file():raise SystemExit('未找到版本工程：'+str(base))
  command=['cmd','/d','/c','gradlew.bat','build','--no-daemon','--no-watch-fs','--console=plain']
  if args.offline:command.append('--offline')
  result=subprocess.run(command,cwd=base,env=env,creationflags=subprocess.CREATE_NO_WINDOW)
  if result.returncode:raise SystemExit(result.returncode)
  print('构建及回归通过：'+mc,flush=True)
if __name__=='__main__':main()
