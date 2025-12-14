@echo off
title Kahoot Desktop - Start All
cd /d %~dp0
if not exist "target\classes" call mvn clean compile -q
start "Kahoot Server" cmd /k "mvn exec:java -Dexec.mainClass=com.example.kahoot.server.ServerMain"
timeout /t 4 /nobreak >nul
start "Kahoot Client" cmd /k "mvn javafx:run"

