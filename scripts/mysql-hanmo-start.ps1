$ErrorActionPreference = 'Stop'
$port = 3307
$bin  = 'D:\devtools\mysql-8.0.28-winx64\bin\mysqld.exe'
$ini  = 'D:\devtools\mysql-hanmo\my.ini'

function Test-Port {
    [bool](Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
}

if (Test-Port) {
    Write-Host "[MySQL] 已在运行 127.0.0.1:3307"
    exit 0
}

Start-Process -FilePath $bin -ArgumentList "--defaults-file=`"$ini`"" -WindowStyle Hidden
Write-Host "[MySQL] 启动中，等待 3307 就绪..."

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    if (Test-Port) {
        Write-Host "[MySQL] 就绪：127.0.0.1:3307  db=hanmo_calligraphy  user=hanmo"
        exit 0
    }
}

Write-Host "[MySQL] 启动超时，请查看 D:\devtools\mysql-hanmo\mysql-error.log"
exit 1
