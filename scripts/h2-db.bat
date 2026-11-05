@echo off
rem Init / rebuild the local H2 file database (server\data\hanmo.mv.db).
rem Usage:  scripts\h2-db.bat            first init (skip if exists)
rem         scripts\h2-db.bat -Reset     drop and rebuild with demo data
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0h2-db.ps1" %*
exit /b %errorlevel%
