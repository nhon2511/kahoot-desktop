@echo off
title Kahoot Desktop
color 0B

echo.
echo ========================================
echo    KAHOOT DESKTOP - ONE CLICK START
echo ========================================
echo.

cd /d %~dp0

REM Kiểm tra đã compile chưa
if not exist "target\classes" (
    echo Dang compile project lan dau...
    call mvn clean compile -q
    if %ERRORLEVEL% NEQ 0 (
        echo Loi khi compile!
        pause
        exit /b 1
    )
) else (
    echo Project da duoc compile, bo qua buoc compile...
)

echo.
echo Dang khoi dong Server...
start "Kahoot Server" /MIN cmd /c "mvn exec:java -Dexec.mainClass=com.example.kahoot.server.ServerMain"

timeout /t 4 /nobreak >nul

echo Dang khoi dong Client...
start "Kahoot Client" cmd /c "mvn javafx:run"

echo.
echo ========================================
echo    Da khoi dong thanh cong!
echo ========================================
echo.
echo Server: Dang chay trong cua so rieng (minimized)
echo Client: Dang chay trong cua so rieng
echo.
echo Nhan phim bat ky de dong cua so nay...
pause >nul

