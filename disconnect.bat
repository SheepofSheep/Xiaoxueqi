@echo off
title Taiyuan Food Seeker - Stop

echo ========================================
echo   Stopping all services...
echo ========================================
echo.

echo [1/2] Stopping backend (Java)...
taskkill /f /im java.exe 2>nul
if %errorlevel% equ 0 (
    echo   Backend stopped.
) else (
    echo   No Java process found.
)

echo [2/2] Stopping frontend (Node)...
taskkill /f /im node.exe 2>nul
if %errorlevel% equ 0 (
    echo   Frontend stopped.
) else (
    echo   No Node process found.
)

echo.
echo All services stopped.
echo.
pause
