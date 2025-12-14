# PowerShell script to run both Server and Client
Set-Location $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   KAHOOT DESKTOP - ONE CLICK START" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path "target\classes")) {
    Write-Host "Dang compile project lan dau..." -ForegroundColor Yellow
    mvn clean compile -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Loi khi compile!" -ForegroundColor Red
        pause
        exit 1
    }
}

Write-Host "Dang khoi dong Server..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-File", "$PSScriptRoot\run-server.ps1" -WindowStyle Minimized

Start-Sleep -Seconds 4

Write-Host "Dang khoi dong Client..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-File", "$PSScriptRoot\run-client.ps1"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Da khoi dong thanh cong!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Server: Dang chay trong cua so rieng (minimized)" -ForegroundColor Yellow
Write-Host "Client: Dang chay trong cua so rieng" -ForegroundColor Yellow
Write-Host ""
Write-Host "Nhan phim bat ky de dong cua so nay..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")






