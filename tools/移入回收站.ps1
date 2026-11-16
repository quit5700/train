param([Parameter(Mandatory=$true)][string]$Manifest, [switch]$SelfTest, [string]$ProjectRoot, [switch]$PreparationOnly)
$ErrorActionPreference='Stop'
[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) { $ProjectRoot=Join-Path $PSScriptRoot '../..' }
$taskProjectRoot=[System.IO.Path]::GetFullPath($ProjectRoot)
$taskSourceRoot=Join-Path $taskProjectRoot '最新正式源码'
$taskDelivery=Join-Path $taskProjectRoot '正式交付/0.2.42'
$taskPlan=Get-Content -LiteralPath $Manifest -Raw -Encoding UTF8 | ConvertFrom-Json
if (-not $SelfTest -and -not $PreparationOnly) {
    $taskReceipt=Get-Content -LiteralPath (Join-Path $taskDelivery 'GitHub发布核验.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    if (-not $taskReceipt.verified) { throw '正式发布未核验，拒绝清理。' }
}
if ($PreparationOnly -and @($taskPlan.entries | Where-Object {$_.kind -ne 'obsolete_asset_copy'}).Count -gt 0) {throw '准备阶段只允许回收新复制的无用素材目录。'}
# IFileOperation（系统文件操作）采用RECYCLEONDELETE，只请求回收站，不提供永久删除回退。
# https://learn.microsoft.com/windows/win32/api/shobjidl_core/nf-shobjidl_core-ifileoperation-setoperationflags
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
namespace TrainRecycle {
 [ComImport,Guid("43826D1E-E718-42EE-BC55-A1E261C37BFE"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] public interface IShellItem {}
 [ComImport,Guid("947AAB5F-0A5C-4C13-B4D6-4BF7836FC9F8"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] public interface IFileOperation {
  [PreserveSig] int Advise(IntPtr sink,out uint cookie);
  [PreserveSig] int Unadvise(uint cookie);
  [PreserveSig] int SetOperationFlags(uint flags);
  [PreserveSig] int SetProgressMessage([MarshalAs(UnmanagedType.LPWStr)] string text);
  [PreserveSig] int SetProgressDialog(IntPtr dialog);
  [PreserveSig] int SetProperties(IntPtr properties);
  [PreserveSig] int SetOwnerWindow(IntPtr hwnd);
  [PreserveSig] int ApplyPropertiesToItem(IShellItem item);
  [PreserveSig] int ApplyPropertiesToItems(IntPtr items);
  [PreserveSig] int RenameItem(IShellItem item,[MarshalAs(UnmanagedType.LPWStr)] string name,IntPtr sink);
  [PreserveSig] int RenameItems(IntPtr items,[MarshalAs(UnmanagedType.LPWStr)] string name);
  [PreserveSig] int MoveItem(IShellItem item,IShellItem folder,[MarshalAs(UnmanagedType.LPWStr)] string name,IntPtr sink);
  [PreserveSig] int MoveItems(IntPtr items,IShellItem folder);
  [PreserveSig] int CopyItem(IShellItem item,IShellItem folder,[MarshalAs(UnmanagedType.LPWStr)] string name,IntPtr sink);
  [PreserveSig] int CopyItems(IntPtr items,IShellItem folder);
  [PreserveSig] int DeleteItem(IShellItem item,IntPtr sink);
  [PreserveSig] int DeleteItems(IntPtr items);
  [PreserveSig] int NewItem(IShellItem folder,uint attributes,[MarshalAs(UnmanagedType.LPWStr)] string name,[MarshalAs(UnmanagedType.LPWStr)] string template,IntPtr sink);
  [PreserveSig] int PerformOperations();
  [PreserveSig] int GetAnyOperationsAborted([MarshalAs(UnmanagedType.Bool)] out bool aborted);
 }
 public static class SafeBin {
  [DllImport("shell32.dll",CharSet=CharSet.Unicode,PreserveSig=true)] static extern int SHCreateItemFromParsingName(string path,IntPtr binding,ref Guid iid,[MarshalAs(UnmanagedType.Interface)] out IShellItem item);
  public static void Recycle(string path) {
   IFileOperation op=null;IShellItem item=null;
   try {
    op=(IFileOperation)Activator.CreateInstance(Type.GetTypeFromCLSID(new Guid("3AD05575-8857-4850-9277-11B85BDB8E09")));
    Marshal.ThrowExceptionForHR(op.SetOperationFlags(0x00080000u|0x20000000u|0x00100000u|0x00000400u|0x00000004u|0x00000010u));
    Guid iid=new Guid("43826D1E-E718-42EE-BC55-A1E261C37BFE");Marshal.ThrowExceptionForHR(SHCreateItemFromParsingName(path,IntPtr.Zero,ref iid,out item));
    Marshal.ThrowExceptionForHR(op.DeleteItem(item,IntPtr.Zero));Marshal.ThrowExceptionForHR(op.PerformOperations());bool aborted;Marshal.ThrowExceptionForHR(op.GetAnyOperationsAborted(out aborted));if(aborted)throw new InvalidOperationException("回收操作中止，没有永久删除回退。");
   } finally {if(item!=null)Marshal.FinalReleaseComObject(item);if(op!=null)Marshal.FinalReleaseComObject(op);}
  }
 }
}
'@
$taskJournal=Join-Path $taskDelivery $(if($SelfTest){'验证记录/回收接口自检.json'}elseif($PreparationOnly){'验证记录/素材整理回收.json'}else{'回收执行记录.json'})
$taskResults=[System.Collections.Generic.List[object]]::new()
if ((Test-Path -LiteralPath $taskJournal) -and -not $SelfTest) {
    foreach($taskOld in @(Get-Content -LiteralPath $taskJournal -Raw -Encoding UTF8 | ConvertFrom-Json)) {$taskResults.Add($taskOld)}
}
$taskOwned=Get-Content -LiteralPath (Join-Path $taskDelivery '清理归属清单.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$taskAllowed=[System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach($taskEntry in $taskOwned.recycle_candidates) {[void]$taskAllowed.Add([System.IO.Path]::GetFullPath($taskEntry.path))}
foreach($taskEntry in $taskPlan.entries) {
    $taskFull=[System.IO.Path]::GetFullPath([string]$taskEntry.path)
    $taskKind=[string]$taskEntry.kind
    if ($taskFull -eq [System.IO.Path]::GetPathRoot($taskFull)) { throw "禁止操作盘符根目录：$taskFull" }
    $taskApproved=$false
    if($SelfTest) {$taskApproved=($taskFull -eq (Join-Path $taskDelivery '验证记录/回收站 中文接口自检'))}
    elseif($taskKind -eq 'owned_old_directory') {$taskApproved=$taskAllowed.Contains($taskFull)}
    elseif($taskKind -eq 'old_mod_file') {
        $taskListed=Get-Content -LiteralPath (Join-Path $taskDelivery '待回收运行包.json') -Raw -Encoding UTF8 | ConvertFrom-Json
        $taskApproved=(@($taskListed | Where-Object {[System.IO.Path]::GetFullPath($_.path) -eq $taskFull}).Count -eq 1)
    }
    elseif($taskKind -eq 'generated_build') {
        $taskApproved=($taskFull -match '^H:\\CodexWork\\模拟火车模组\\最新正式源码\\versions\\0\.2\.42\\mc(1\.20\.[14]|1\.21\.(1|4|8|10|11)|26\.1\.2|26\.2|26\.3)\\(build|\.gradle|dist|run-smoke)$')
    }
    elseif($taskKind -eq 'obsolete_asset_copy') {$taskApproved=($taskFull -in @((Join-Path $taskSourceRoot 'design/保留素材/logical-world-preview-1793512504'),(Join-Path $taskSourceRoot 'design/保留素材/外观修正')))}
    elseif($taskKind -eq 'one_off_script') {$taskApproved=($taskFull -in @((Join-Path $taskProjectRoot '正式发布管理.py'),(Join-Path $taskProjectRoot '清理准备.py'),(Join-Path $taskProjectRoot '__pycache__'),(Join-Path $taskSourceRoot 'tools/__pycache__')))}
    elseif($taskKind -eq 'generated_jvm_temp') {$taskApproved=($taskFull -eq 'H:\CodexWork\q5-gradle-cache\train-formal-temp')}
    if(-not $taskApproved) {throw "未在本模组明确白名单内：$taskFull"}
    if(-not (Test-Path -LiteralPath $taskFull)) {continue}
    $taskItem=Get-Item -LiteralPath $taskFull -Force
    if(($taskItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {throw "链接目录需单独核对，不自动递归：$taskFull"}
    try {
        [TrainRecycle.SafeBin]::Recycle($taskFull)
        if(Test-Path -LiteralPath $taskFull){throw '回收后原路径仍存在。'}
        $taskResults.Add([pscustomobject]@{path=$taskFull;kind=$taskKind;recycled=$true;time=(Get-Date -Format o)})
        Write-Output "已移入回收站：$taskFull"
    } catch {
        $taskResults.Add([pscustomobject]@{path=$taskFull;kind=$taskKind;recycled=$false;time=(Get-Date -Format o);error=$_.Exception.Message})
        [System.IO.File]::WriteAllText($taskJournal, (ConvertTo-Json -InputObject @($taskResults.ToArray()) -Depth 6), [System.Text.UTF8Encoding]::new($false))
        throw
    }
    [System.IO.File]::WriteAllText($taskJournal, (ConvertTo-Json -InputObject @($taskResults.ToArray()) -Depth 6), [System.Text.UTF8Encoding]::new($false))
}
