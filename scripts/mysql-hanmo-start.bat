@echo off
rem Start the dedicated MySQL 8 instance for this project (port 3307).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0mysql-hanmo-start.ps1" %*
exit /b %errorlevel%
