<#
  初始化 / 重建本地 H2 文件数据库（server/data/hanmo.mv.db）

  用法：
    scripts\h2-db.bat             # 首次初始化（已存在则跳过）
    scripts\h2-db.bat -Reset      # 删库重建，恢复演示数据

  说明：
    application-dev.yml 已改为 file 模式且 sql.init.mode=never，
    因此建表 + 灌演示数据统一由本脚本完成，避免每次启动重复 INSERT。
#>
param([switch]$Reset)

$ErrorActionPreference = 'Stop'

$root      = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $root 'server'
$dataDir   = Join-Path $serverDir 'data'
$dbBase    = Join-Path $dataDir 'hanmo'
$dbFile    = "$dbBase.mv.db"
$dbUrlPath = $dbBase -replace '\\', '/'

if ($Reset -and (Test-Path $dataDir)) {
  Get-ChildItem $dataDir -Filter 'hanmo*' -File | Remove-Item -Force
  Write-Host "[H2] 已删除旧数据库文件，准备重建" -ForegroundColor Yellow
}

if (Test-Path $dbFile) {
  Write-Host "[H2] 数据库已存在：$dbFile" -ForegroundColor Green
  Write-Host "     如需按最新 SQL 重建，请执行 scripts\h2-db.bat -Reset"
  return
}

# ---- 定位 H2 驱动 ----
# 优先从已构建的 fat jar 里取，保证与后端运行版本完全一致；其次扫 Maven 仓库
$h2Jar = $null
$fatJar = Join-Path $serverDir 'target\hanmo-server.jar'
if (Test-Path $fatJar) {
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $zip = [System.IO.Compression.ZipFile]::OpenRead($fatJar)
  try {
    $entry = $zip.Entries | Where-Object { $_.FullName -match '^BOOT-INF/lib/h2-.*\.jar$' } | Select-Object -First 1
    if ($entry) {
      $cacheDir = Join-Path $serverDir 'target\h2-init'
      New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
      $h2Jar = Join-Path $cacheDir (Split-Path $entry.FullName -Leaf)
      if (-not (Test-Path $h2Jar)) {
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $h2Jar, $true)
      }
    }
  } finally { $zip.Dispose() }
}
if (-not $h2Jar) {
  $repos = @(
    (Join-Path $env:USERPROFILE '.m2\repository'),
    'D:\devtools\maven-repo'
  )
  $h2Jar = $repos |
    ForEach-Object { Get-ChildItem (Join-Path $_ 'com\h2database\h2') -Recurse -Filter 'h2-*.jar' -ErrorAction SilentlyContinue } |
    Where-Object { $_.Name -notmatch 'sources|javadoc' } |
    Sort-Object { [version]([regex]::Match($_.BaseName, '\d+\.\d+\.\d+').Value) } -Descending |
    Select-Object -First 1 -ExpandProperty FullName
}
if (-not $h2Jar) {
  throw "未找到 H2 驱动。请先在 server 目录执行一次：mvn -q -DskipTests package"
}

New-Item -ItemType Directory -Force -Path $dataDir | Out-Null

$url    = "jdbc:h2:file:$dbUrlPath;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE,USER"
$schema = Join-Path $serverDir 'src\main\resources\db\schema-h2.sql'
$data   = Join-Path $serverDir 'src\main\resources\db\data-h2.sql'

Write-Host "[H2] 驱动：$h2Jar"

Write-Host "[H2] 建表 ..."
& java -cp $h2Jar org.h2.tools.RunScript -url $url -user sa -script $schema
if ($LASTEXITCODE -ne 0) { throw "建表失败，退出码 $LASTEXITCODE" }

Write-Host "[H2] 写入演示数据 ..."
& java -cp $h2Jar org.h2.tools.RunScript -url $url -user sa -script $data
if ($LASTEXITCODE -ne 0) { throw "写入演示数据失败，退出码 $LASTEXITCODE" }

$tableCount = (Select-String -Path $schema -Pattern 'CREATE TABLE').Count
Write-Host "[H2] 完成：$tableCount 张表，数据文件 $dbFile" -ForegroundColor Green
Write-Host "     后台账号：admin / Admin@123456，jiaowu / Jiaowu@123456"
