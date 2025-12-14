@echo off
title Kahoot Client
cd /d %~dp0
if not exist "target\classes" call mvn clean compile -q
mvn javafx:run

