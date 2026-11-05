# ============================================================
# 注释改造验收脚本
#   1) 检查 git diff 中是否有「删除行」——有就说明动到了代码
#   2) 统计各模块注释密度
#   3) 检查源文件是否有文件头注释
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts\check-comments.ps1
# 说明：本机 Windows PowerShell 5.1 读取含中文的 .ps1 需要 UTF-8 BOM，请勿另存为无 BOM。
# ============================================================

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "=== 1. 检查是否有代码被删除/修改（应为 0）===" -ForegroundColor Cyan

# git 在 core.autocrlf=true 时会往 stderr 打 warning，这里统一过滤掉
$numstat = & git diff --numstat 2>&1 | Where-Object { $_ -is [string] -and $_ -notmatch '^warning:' }

$violations = New-Object System.Collections.ArrayList
foreach ($line in $numstat) {
    if ($line -match '^(\d+)\s+(\d+)\s+(.+)$') {
        $deleted = [int]$Matches[2]
        if ($deleted -gt 0) {
            [void]$violations.Add(("{0}  删除 {1} 行" -f $Matches[3], $deleted))
        }
    }
}

if ($violations.Count -eq 0) {
    Write-Host "  OK：没有删除行，代码未被修改" -ForegroundColor Green
} else {
    Write-Host "  警告：以下文件有删除行，需人工复核" -ForegroundColor Yellow
    $violations | ForEach-Object { Write-Host "    $_" }
}

Write-Host ""
Write-Host "=== 2. 各模块注释密度 ===" -ForegroundColor Cyan
$areas = @(
    @{ Name = '后端 Java';   Path = 'server/src/main/java'; Ext = '*.java' },
    @{ Name = '管理端 Vue';  Path = 'admin-web/src';        Ext = '*.vue' },
    @{ Name = '管理端 JS';   Path = 'admin-web/src';        Ext = '*.js' },
    @{ Name = '小程序 JS';   Path = 'miniprogram';          Ext = '*.js' },
    @{ Name = '小程序 WXML'; Path = 'miniprogram';          Ext = '*.wxml' },
    @{ Name = '小程序 WXSS'; Path = 'miniprogram';          Ext = '*.wxss' }
)
foreach ($a in $areas) {
    $files = Get-ChildItem $a.Path -Recurse -Filter $a.Ext -ErrorAction SilentlyContinue
    $lines = 0; $comment = 0
    foreach ($f in $files) {
        $content = Get-Content $f.FullName -Encoding UTF8 -ErrorAction SilentlyContinue
        $lines += $content.Count
        $comment += ($content | Where-Object { $_ -match '^\s*(//|/\*|\*|<!--|#)' }).Count
    }
    $density = if ($lines -gt 0) { $comment / $lines } else { 0 }
    Write-Host ("  {0,-12} 文件 {1,3}  代码行 {2,6}  注释行 {3,5}  密度 {4,6:P1}" -f $a.Name, $files.Count, $lines, $comment, $density)
}

Write-Host ""
Write-Host "=== 3. 缺少文件头注释的文件 ===" -ForegroundColor Cyan
$checkList = @(
    @{ Path = 'server/src/main/java'; Ext = '*.java' },
    @{ Path = 'admin-web/src/views';  Ext = '*.vue' },
    @{ Path = 'miniprogram/pages';    Ext = '*.js' }
)
$missing = New-Object System.Collections.ArrayList
foreach ($item in $checkList) {
    foreach ($f in Get-ChildItem $item.Path -Recurse -Filter $item.Ext -ErrorAction SilentlyContinue) {
        $head = Get-Content $f.FullName -TotalCount 80 -Encoding UTF8 -ErrorAction SilentlyContinue
        if (-not ($head | Where-Object { $_ -match '^\s*(/\*\*|/\*|<!--|\*)' })) {
            [void]$missing.Add($f.FullName.Replace("$root\", ''))
        }
    }
}
if ($missing.Count -eq 0) {
    Write-Host "  OK：所有源文件都有文件头注释" -ForegroundColor Green
} else {
    Write-Host ("  有 {0} 个文件缺少文件头注释：" -f $missing.Count) -ForegroundColor Yellow
    $missing | Select-Object -First 20 | ForEach-Object { Write-Host "    $_" }
}
