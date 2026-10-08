# -*- coding: utf-8 -*-
"""生成两版独立测试交付及当前对话交接，不安装或发布。"""
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET
import zipfile
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION = '0.2.41-test1'
HANDOFF = Path('H:/Codex交接文档库/火车_网易基岩转换_20260930/Java轨迹修复_0.2.41-test1')

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    global VERSION, HANDOFF
    parser=argparse.ArgumentParser()
    parser.add_argument('--version',choices=('0.2.41-test1','0.2.41'),default='0.2.41-test1')
    args=parser.parse_args()
    VERSION=args.version
    HANDOFF=HANDOFF.parent/('Java轨迹修复_'+VERSION)
    results = []
    for mc in ('1.20.1', '1.20.4'):
        base = ROOT / ('mc'+mc)
        reports = list((base/'build/test-results/test').glob('TEST-*.xml'))
        if not reports: raise ValueError('缺少测试报告：'+mc)
        totals = {key: 0 for key in ('tests','failures','errors','skipped')}
        cleanup = False
        for file in reports:
            suite = ET.parse(file).getroot()
            for key in totals: totals[key] += int(suite.get(key,'0'))
            if suite.get('name','').endswith('.TrackTrailCleanupTest'):
                cleanup = int(suite.get('tests','0')) == 2
        if not cleanup or any(totals[key] for key in ('failures','errors','skipped')):
            raise ValueError('完整测试未通过：'+mc)
        target = base/'dist'/VERSION
        target.mkdir(parents=True,exist_ok=True)
        files = []
        for suffix in ('fabric.jar','fabric-sources.jar'):
            name = 'fabric-train-simulator-'+VERSION+'-mc'+mc+'-'+suffix
            source = base/'dist'/name
            with zipfile.ZipFile(source) as archive:
                if archive.testzip(): raise ValueError('运行或源码归档损坏')
                if suffix == 'fabric.jar':
                    meta=json.loads(archive.read('fabric.mod.json').decode('utf-8'))
                    if meta['version'] != VERSION: raise ValueError('包内版本不一致')
                    entry='cn/quit5700/fabrictrain/train/TrackTrail.class'
                    if b'removeLast' not in archive.read(entry): raise ValueError('修复未进入运行包')
                else:
                    entry='cn/quit5700/fabrictrain/train/TrackTrail.java'
                    text=archive.read(entry).decode('utf-8')
                    if text.count('points.removeLast()') != 2 or 'points.remove(points.size()' in text:
                        raise ValueError('源码包未同步修复')
            destination=target/name
            shutil.copyfile(source,destination)
            files.append(destination)
        source_zip=target/('模拟火车-'+VERSION+'-mc'+mc+'-完整工程.zip')
        with zipfile.ZipFile(source_zip,'w',zipfile.ZIP_DEFLATED) as output:
            for file in sorted(base.rglob('*')):
                relative=file.relative_to(base)
                if file.is_file() and not {'.gradle','.manual-build','build','run','dist','logs'}.intersection(relative.parts):
                    output.write(file,'mc'+mc+'/'+relative.as_posix())
        with zipfile.ZipFile(source_zip) as archive:
            if archive.testzip(): raise ValueError('完整工程损坏')
        files.append(source_zip)
        for name in ('README.md','VERSION_INFO.md'):
            shutil.copyfile(base/name,target/name)
        shutil.copytree(base/'build/test-results/test',target/'测试报告',dirs_exist_ok=True)
        result={'minecraft':mc,'mod_version':VERSION,'type':'正式版，用户已明确确认' if VERSION=='0.2.41' else '测试版',
                'java':'17','fabric_loader':'0.15.11','fabric_loom':'1.6.12',
                'fabric_api':'0.92.9+1.20.1' if mc=='1.20.1' else '0.97.3+1.20.4',
                'yarn_mappings':'未使用；Mojang官方命名映射 '+mc,
                'full_test_results':totals,'track_module_tests':5,'game_server_verified':False,
                'files':{file.name:sha(file) for file in files}}
        (target/'验证结果.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        shutil.copytree(target,HANDOFF/('mc'+mc),dirs_exist_ok=True)
        old=HANDOFF/'旧0.2.40保留'/('mc'+mc)
        old.mkdir(parents=True,exist_ok=True)
        for file in (base/'dist').glob('*-0.2.40-mc'+mc+'-*.jar'):
            shutil.copyfile(file,old/file.name)
        results.append(result)
    shutil.copytree(ROOT/'docs/bugs',HANDOFF/'问题记录',dirs_exist_ok=True)
    shutil.copytree(ROOT/'tools',HANDOFF/'构建工具',dirs_exist_ok=True,ignore=shutil.ignore_patterns('__pycache__'))
    repo_zip=HANDOFF/'本轮LWJGL3.3.1构建依赖.zip'
    repo=ROOT/'outputs/verified-lwjgl-repository'
    with zipfile.ZipFile(repo_zip,'w',zipfile.ZIP_DEFLATED) as output:
        for file in sorted(repo.rglob('*')):
            if file.is_file() and '3.3.1' in file.relative_to(repo).parts:
                output.write(file,'outputs/verified-lwjgl-repository/'+file.relative_to(repo).as_posix())
    with zipfile.ZipFile(repo_zip) as archive:
        if archive.testzip(): raise ValueError('构建依赖归档损坏')
    note='''# 1.20.1与1.20.4轨迹清理修复交接

本轮处理的是Java（爪哇）Fabric（织物）模拟火车，不是网易基岩版Q5铁路。项目根为C:/Users/quit5700/Documents/mc虚拟线路模组/Fabric模拟火车模组。

两版测试交付0.2.41-test1，正式版未经用户确认。0.2.40旧运行包与源码包已另存，Git历史保留。轨迹点双端队列原先使用按对象删除的remove（删除），整数参数不能删除Point（轨迹点），超过140格时循环不推进；两处改为removeLast（删除队尾）。两版修复前的新增测试均超时，修复后分别完成5项轨迹模块测试及完整构建。完整数量、包摘要和依赖版本见每版验证结果.json。

玩家换包前停止服务器、备份世界，然后用同Minecraft（我的世界）版本的运行包替换0.2.40，不要同时保留两个同模组运行包。客户端和服务器应使用同一测试版。文件名含sources（源码）的包和完整工程ZIP（压缩包）不安装进mods（模组目录）。未擅自安装，不发布正式版，不操作用户存档。

实机仍需验证列车累计行驶超过140格、反向运行及长编组跟随。模型、界面和存档格式没有改变。本轮不宣称排除服务器所有其他卡顿原因。

1.20.4普通构建命令：在其目录执行gradlew.bat test build --offline --no-daemon。
1.20.1本轮依赖补齐后，在其目录执行gradlew.bat -I（绝对路径的tools/verified-lwjgl.init.gradle）test build --offline --no-daemon。本地LWJGL（轻量Java游戏库）依赖包解压到工程根目录，构建工具放入根tools目录；其他既有Gradle依赖仍需本地缓存或联网下载。本轮依赖下载失败方式、校验值及中文输出修复见BUG-074。

以后修复先看当前项目根docs/bugs/BUG_INDEX.md和相关条目，不读取其他项目问题记录。下一测试迭代增加test（测试）编号；只有用户明确确认才能生成正式版本号。
'''
    if VERSION=='0.2.41':
        note=note.replace('两版测试交付0.2.41-test1，正式版未经用户确认。',
            '两版正式交付0.2.41，用户于2026-10-08明确确认正式版并授权更新GitHub。与测试版运行功能一致，重新生成正式元数据。1.20.1测试版用户反馈正常，1.20.4仍待实机反馈。')
        note+='\n旧0.2.41-test1测试交付及源码继续保留在本对话的Java轨迹修复_0.2.41-test1目录。\n'
    (HANDOFF/'先读我.md').write_text(note,encoding='utf-8')
    (ROOT/'docs/轨迹清理修复交接.md').write_text(note,encoding='utf-8')
    (HANDOFF/'交付验证.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    # Verify the actual handoff copies rather than only source package hashes.
    for result in results:
        for filename,digest in result['files'].items():
            if sha(HANDOFF/('mc'+result['minecraft'])/filename)!=digest:
                raise ValueError('H盘交付字节不一致')
    print(json.dumps(results,ensure_ascii=False,indent=2))

if __name__=='__main__': main()
