@echo off
cd /d "%~dp0"
call mvn compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.jenga.aeternalis.rendering.BenchmarkMain -Dexec.args=benchmarks
pause
