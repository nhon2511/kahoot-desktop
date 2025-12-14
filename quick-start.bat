@echo off
title Kahoot Desktop Quick Start
color 0A

echo.
echo ========================================
echo    KAHOOT DESKTOP - QUICK START
echo ========================================
echo.
echo Chon che do chay:
echo.
echo [1] Chay Server
echo [2] Chay Client  
echo [3] Chay ca Server va Client (2 cua so)
echo [4] Thoat
echo.
set /p choice="Nhap lua chon (1-4): "

if "%choice%"=="1" (
    call start-server.bat
    goto end
)

if "%choice%"=="2" (
    call start-client.bat
    goto end
)

if "%choice%"=="3" (
    call start-all.bat
    goto end
)

if "%choice%"=="4" (
    exit
)

echo Lua chon khong hop le!
timeout /t 2 >nul

:end






