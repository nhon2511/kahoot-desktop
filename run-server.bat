@echo off
title Kahoot Server
cd /d %~dp0
if not exist "target\classes" call mvn clean compile -q
mvn exec:java -Dexec.mainClass="com.example.kahoot.server.ServerMain"

