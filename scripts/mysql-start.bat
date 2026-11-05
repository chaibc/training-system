@echo off
rem Start the other local MySQL 8 instance (port 3306, legacy project).
rem This one is NOT used by the hanmo project; kept for reference only.
start "MySQL-3306" /MIN "D:\devtools\mysql-8.0.28-winx64\bin\mysqld.exe" --defaults-file="D:\devtools\mysql-8.0.28-winx64\my.ini"
echo [MySQL] start issued for 3306
