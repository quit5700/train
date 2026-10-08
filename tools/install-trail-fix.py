# -*- coding: utf-8 -*-
"""按用户授权备份已确认实例并替换运行包；不结束游戏。"""
import hashlib
import json
import os
import shutil
import zipfile
import argparse
from datetime import datetime, timezone, timedelta
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
GAME=Path('G:/game/mcfabric/.minecraft/versions/1.20.1-Fabric-mtr')
HANDOFF=Path('H:/Codex交接文档库/火车_网易基岩转换_20260930/Java轨迹修复_0.2.41-test1')
SOURCE=ROOT/'mc1.20.1/dist/0.2.41-test1/fabric-train-simulator-0.2.41-test1-mc1.20.1-fabric.jar'

def sha(file):
    value=hashlib.sha256()
    with file.open('rb') as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b''): value.update(chunk)
    return value.hexdigest()

def main():
    global GAME, SOURCE, HANDOFF
    parser=argparse.ArgumentParser()
    parser.add_argument('--reuse-backup',type=Path)
    parser.add_argument('--minecraft-version',choices=('1.20.1','1.20.4'),default='1.20.1')
    parser.add_argument('--game-stopped',action='store_true')
    parser.add_argument('--skip-saves',action='store_true')
    parser.add_argument('--mod-version',choices=('0.2.41-test1','0.2.41'),default='0.2.41-test1')
    parser.add_argument('--previous-version',choices=('0.2.40','0.2.41-test1'),default='0.2.40')
    args=parser.parse_args()
    mc=args.minecraft_version
    mod=args.mod_version
    GAME=Path('G:/game/mcfabric/.minecraft/versions/'+mc+'-Fabric-mtr')
    SOURCE=ROOT/('mc'+mc)/'dist'/mod/('fabric-train-simulator-'+mod+'-mc'+mc+'-fabric.jar')
    HANDOFF=HANDOFF.parent/('Java轨迹修复_'+mod)
    expected={'1.20.1':'50f60e51893b8dec627d9df65559a421a0e5f52340f99c581166111eae843c7a',
              '1.20.4':'3d9b31c3092e680f8b78ce5c39041e5b4b96692a2d17930b443766b44dea6189'}
    if mod=='0.2.41':
        result=json.loads((SOURCE.parent/'验证结果.json').read_text(encoding='utf-8'))
        if result['mod_version']!=mod or result['minecraft']!=mc: raise ValueError('正式验证记录不匹配')
        expected[mc]=result['files'][SOURCE.name]
    if sha(SOURCE)!=expected[mc]:
        raise ValueError('已验证的运行包摘要不一致')
    mods=(GAME/'mods').resolve()
    old=mods/('fabric-train-simulator-'+args.previous_version+'-mc'+mc+'-fabric.jar')
    new=mods/SOURCE.name
    if not old.exists() or new.exists(): raise ValueError('安装前状态变化，请重新检查')
    stamp=datetime.now(timezone(timedelta(hours=8))).strftime('%Y%m%d_%H%M%S')
    backup=args.reuse_backup.resolve() if args.reuse_backup else ROOT/'outputs'/('当前实例mc'+mc+'安装前备份_'+stamp+'.zip')
    if backup.resolve().parent!=(ROOT/'outputs').resolve(): raise ValueError('备份路径越界')
    backup.parent.mkdir(parents=True,exist_ok=True)
    skipped=[]
    if not args.reuse_backup:
        with zipfile.ZipFile(backup,'x',zipfile.ZIP_DEFLATED,compresslevel=1) as archive:
            archive.write(old,'mods/'+old.name)
            for file in ([] if args.skip_saves else sorted((GAME/'saves').rglob('*'))):
                if not file.is_file(): continue
                if file.name=='session.lock': skipped.append(file.relative_to(GAME).as_posix()); continue
                archive.write(file,file.relative_to(GAME).as_posix())
            for relative in ('logs/latest.log','options.txt'):
                file=GAME/relative
                if file.exists(): archive.write(file,relative)
    else:
        skipped=[file.relative_to(GAME).as_posix() for file in (GAME/'saves').rglob('session.lock')]
    with zipfile.ZipFile(backup) as archive:
        if archive.testzip(): raise ValueError('安装前备份损坏，停止替换')
        if hashlib.sha256(archive.read('mods/'+old.name)).hexdigest()!=sha(old):
            raise ValueError('备份中旧包与当前旧包不一致')
    archived_backup=HANDOFF/'安装前备份'/backup.name
    archived_backup.parent.mkdir(parents=True,exist_ok=True)
    if not archived_backup.exists(): shutil.copyfile(backup,archived_backup)
    if sha(backup)!=sha(archived_backup): raise ValueError('H盘备份校验失败，停止替换')
    disabled=old.with_name(old.name+'.disabled_'+stamp)
    staged=new.with_name(new.name+'.install_tmp')
    for file in (old,new,disabled,staged):
        if file.resolve().parent!=mods: raise ValueError('安装路径越界')
    shutil.copyfile(SOURCE,staged)
    if sha(staged)!=sha(SOURCE): raise ValueError('暂存运行包校验失败')
    try:
        old.rename(disabled)
        try: os.replace(staged,new)
        except Exception:
            disabled.rename(old)
            raise
    finally:
        if staged.exists(): staged.unlink()
    if sha(new)!=sha(SOURCE): raise ValueError('安装后字节校验失败')
    active=[]
    for file in mods.glob('*.jar'):
        with zipfile.ZipFile(file) as archive:
            if 'fabric.mod.json' in archive.namelist():
                meta=json.loads(archive.read('fabric.mod.json').decode('utf-8-sig'))
                if meta.get('id')=='fabric_train_simulator': active.append({'file':file.name,'version':meta['version']})
    if len(active)!=1 or active[0]['version']!=mod:
        raise ValueError('仍存在重复或错误的列车模组运行包')
    report={'game_directory':str(GAME),'installed_version':mod,'active_mods':active,
            'disabled_old_package':str(disabled),'backup':str(backup),'h_drive_backup':str(archived_backup),
            'backup_sha256':sha(backup),'skipped_lock_files':skipped,
            'saves_backed_up':not args.skip_saves,
            'backup_is_live_disk_snapshot':not args.game_stopped,'restart_required':True,'game_verified':False,
            'installed_sha256':sha(new)}
    text=json.dumps(report,ensure_ascii=False,indent=2)+'\n'
    filename='当前实例安装记录_mc'+mc+'_'+mod+'.json'
    (ROOT/'docs'/filename).write_text(text,encoding='utf-8')
    (HANDOFF/filename).write_text(text,encoding='utf-8')
    print(text)

if __name__=='__main__': main()
