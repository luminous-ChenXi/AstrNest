@echo off
chcp 65001 >nul
echo ==========================================
echo    AstrNest Database Init Tool
echo    Windows Version
echo ==========================================
echo.
echo [INFO] Windows 系统 - 使用 init_windows.sql
echo [INFO] 注意：Windows 版本不包含 CREATE USER/GRANT 语句
echo.

python "%~dp0init-admin.py"

if errorlevel 1 (
    echo.
    echo [ERROR] 初始化失败
    pause
    exit /b 1
)

pause
