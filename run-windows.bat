@echo off
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
 echo Install Apache Maven and add its bin folder to PATH, or run this Maven project in NetBeans.
 pause
 exit /b 1
)
call mvn javafx:run
pause
