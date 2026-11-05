@echo off
rem Stop the dedicated MySQL 8 instance for this project (port 3307).
"D:\devtools\mysql-8.0.28-winx64\bin\mysqladmin.exe" --host=127.0.0.1 --port=3307 --user=root --password=Hanmo@2026 shutdown 2>nul
if %errorlevel%==0 (
  echo [MySQL] stopped
) else (
  echo [MySQL] not running or stop failed
)
