@echo off
setlocal
cd /d "%~dp0"
call mvn compile dependency:build-classpath -Dmdep.outputFile=target\network-classpath.txt
if errorlevel 1 exit /b 1
set /p AETERNALIS_CP=<target\network-classpath.txt
java -Xmx1g -cp "target\classes;%AETERNALIS_CP%" com.jenga.aeternalis.distributed.NetworkBenchmark benchmarks\network
pause
