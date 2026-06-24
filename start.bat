@echo off
title Taiyuan Food Seeker - Start

if exist "%~dp0.env" (
    echo Loading local .env...
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do (
        if not "%%A"=="" if not defined %%A set "%%A=%%B"
    )
)

if not defined DEEPSEEK_ENABLED set DEEPSEEK_ENABLED=false
if not defined DEEPSEEK_BASE_URL set DEEPSEEK_BASE_URL=https://api.deepseek.com
if not defined DEEPSEEK_MODEL set DEEPSEEK_MODEL=deepseek-chat
if not defined DEEPSEEK_TIMEOUT_SECONDS set DEEPSEEK_TIMEOUT_SECONDS=15

echo ========================================
echo   Taiyuan Food Seeker
echo ========================================
echo.
if not defined AMAP_WEB_SERVICE_KEY (
    echo WARNING: AMAP_WEB_SERVICE_KEY is not set. Map and Amap import will degrade.
)
if "%DEEPSEEK_ENABLED%"=="true" if not defined DEEPSEEK_API_KEY (
    echo WARNING: DEEPSEEK_ENABLED=true but DEEPSEEK_API_KEY is not set. AI polishing will degrade.
)
echo.
echo [1/2] Starting backend...

cd /d "%~dp0backend"
if not exist "target\food-seeker-0.0.1-SNAPSHOT.jar" (
    echo ERROR: JAR not found, run: mvn package -DskipTests
    pause
    exit /b 1
)

start "FoodSeeker-Backend" java -jar "target\food-seeker-0.0.1-SNAPSHOT.jar"

echo Waiting for backend (8s)...
timeout /t 8 /nobreak >nul

echo [2/2] Starting frontend...
cd /d "%~dp0frontend"
start "FoodSeeker-Frontend" cmd /c "pnpm dev --host 0.0.0.0"

echo.
echo ========================================
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:5173
echo   Phone:    http://YOUR-IP:5173
echo ========================================
echo.
echo Run disconnect.bat to stop all.
pause
