# PowerShell script to run Client
Set-Location $PSScriptRoot

if (-not (Test-Path "target\classes")) {
    Write-Host "Dang compile project..." -ForegroundColor Yellow
    mvn clean compile -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Loi khi compile!" -ForegroundColor Red
        pause
        exit 1
    }
}

Write-Host "Dang khoi dong Client..." -ForegroundColor Green
mvn javafx:run






