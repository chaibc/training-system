@echo off
rem Stop the other local MySQL 8 instance (port 3306). It will ask for the root password.
set "MYSQL_HOME=D:\devtools\mysql-8.0.28-winx64"

"%MYSQL_HOME%\bin\mysqladmin.exe" --host=127.0.0.1 --port=3306 --user=root -p shutdown
if %errorlevel%==0 (
  echo [MySQL] stopped
) else (
  echo [MySQL] not running or wrong root password
)
