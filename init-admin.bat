@echo off
chcp 65001 >nul
setlocal ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION
set ROOT_DIR=%~dp0
set SQL_MAIN=%ROOT_DIR%backend\db\init.sql
set SQL_WIN=%ROOT_DIR%backend\db\init_windows.sql

echo ==========================================
echo    AstrNest Database Init Tool
echo ==========================================
echo.

:: Check Python
where python >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Python not found. Please install Python 3 first.
    echo Download: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo [1/4] Database Connection Config
echo.
echo Select initialization type:
echo   1. Full init (create DB + user + tables) - requires root
echo   2. Tables only (for existing DB) - normal permission
echo.
set /p INIT_TYPE="Select [1/2]: "

if "!INIT_TYPE!"=="1" (
    set FULL_INIT=1
    echo.
    echo [INFO] Full init requires MySQL root account
) else (
    set FULL_INIT=0
    echo.
    echo [INFO] Tables only mode, make sure DB exists
)

echo.
echo MySQL Host [default: localhost]:
set /p DB_HOST="> "
if "!DB_HOST!"=="" set DB_HOST=localhost

echo MySQL Port [default: 3306]:
set /p DB_PORT="> "
if "!DB_PORT!"=="" set DB_PORT=3306

if "!FULL_INIT!"=="1" (
    echo MySQL root username [default: root]:
    set /p DB_ROOT_USER="> "
    if "!DB_ROOT_USER!"=="" set DB_ROOT_USER=root
    
    echo MySQL root password:
    set /p DB_ROOT_PASS="> "
) else (
    echo Database name [default: astrnest]:
    set /p DB_NAME="> "
    if "!DB_NAME!"=="" set DB_NAME=astrnest
    
    echo Database username:
    set /p DB_USER="> "
    
    echo Database password:
    set /p DB_PASS="> "
)

echo.
echo ==========================================
echo [2/4] Admin Account Config
echo ==========================================
echo.
echo Admin username [default: admin]:
set /p ADMIN_USER="> "
if "!ADMIN_USER!"=="" set ADMIN_USER=admin

echo Admin email [default: admin@example.com]:
set /p ADMIN_EMAIL="> "
if "!ADMIN_EMAIL!"=="" set ADMIN_EMAIL=admin@example.com

echo Admin password (required, no default):
set /p ADMIN_PASS="> "
if "!ADMIN_PASS!"=="" (
    echo [ERROR] Admin password cannot be empty.
    pause
    exit /b 1
)

echo Confirm password:
set /p ADMIN_PASS2="> "

if not "!ADMIN_PASS!"=="!ADMIN_PASS2!" (
    echo [ERROR] Passwords do not match. Exit.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo [3/4] Generating encrypted password...
echo ==========================================
echo.

:: Create temp Python script for encryption
set TMPPY=%TEMP%\astrnest_admin_%~n0_%RANDOM%.py
(
echo import sys
echo try:
echo     import bcrypt
echo except ImportError:
echo     print("[INFO] Installing bcrypt...")
echo     import subprocess
echo     subprocess.check_call([sys.executable, "-m", "pip", "install", "bcrypt", "-q"])
echo     import bcrypt
echo password = sys.argv[1]
echo hashed = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt(rounds=12)).decode('utf-8')
echo print(hashed)
) > "%TMPPY%"

:: Execute encryption
set PYTHONIOENCODING=utf-8
set PYTHONUTF8=1
for /f "delims=" %%i in ('python "%TMPPY%" "!ADMIN_PASS!"') do set HASHED_PASS=%%i
del "%TMPPY%"

if "!HASHED_PASS!"=="" (
    echo [ERROR] Password encryption failed
    pause
    exit /b 1
)

echo [OK] Password encrypted (bcrypt)
echo.

echo ==========================================
echo [4/4] Updating SQL files...
echo ==========================================
echo.

:: Create Python script to update SQL files
set TMPPY2=%TEMP%\astrnest_sql_%~n0_%RANDOM%.py
(
echo import re
echo import sys
echo from pathlib import Path
echo def update_sql(file_path, admin_user, admin_email, hashed_pass):
echo     text = Path(file_path).read_text(encoding='utf-8')
echo     user_sql = admin_user.replace("'", "''")
echo     email_sql = admin_email.replace("'", "''")
echo     new_insert = f"""INSERT INTO users (id, username, password, nickname, email, active, created_at)
echo VALUES (1, '{user_sql}', '{hashed_pass}', 'Super Admin', '{email_sql}', 1, NOW())
echo ON DUPLICATE KEY UPDATE
echo     password = VALUES(password),
echo     nickname = VALUES(nickname),
echo     email = VALUES(email),
echo     active = 1;"""
echo     pattern = re.compile(r"INSERT INTO users \(id, username, password, nickname, email, active, created_at\)\s*VALUES \(1, '.*?', '.*?', '.*?', '.*?', 1, NOW\(\)\)\s*ON DUPLICATE KEY UPDATE\s*password = VALUES\(password\),\s*nickname = VALUES\(nickname\),\s*email = VALUES\(email\),\s*active = 1;", re.S)
echo     if not pattern.search(text):
echo         print(f"[WARN] Admin insert not found: {file_path}")
echo         return False
echo     new_text = pattern.sub(new_insert, text)
echo     Path(file_path).write_text(new_text, encoding='utf-8')
echo     print(f"[OK] Updated: {file_path}")
echo     return True
echo if __name__ == "__main__":
echo     update_sql(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
echo     update_sql(sys.argv[5], sys.argv[2], sys.argv[3], sys.argv[4])
) > "%TMPPY2%"

python "%TMPPY2%" "!SQL_MAIN!" "!ADMIN_USER!" "!ADMIN_EMAIL!" "!HASHED_PASS!" "!SQL_WIN!"
del "%TMPPY2%"

echo.

:: Execute database initialization
if "!FULL_INIT!"=="1" (
    echo ==========================================
    echo [5/5] Executing database initialization...
    echo ==========================================
    echo.
    echo [INFO] Using root account to create DB and user...
    echo.
    
    set TMPSQL=%TEMP%\astrnest_init_%~n0_%RANDOM%.sql
    
    (
    echo CREATE DATABASE IF NOT EXISTS astrnest CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    echo.
    echo CREATE USER IF NOT EXISTS 'astrnest'@'%%' IDENTIFIED BY '!DB_PASS!';
    echo GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'%%';
    echo FLUSH PRIVILEGES;
    echo.
    type "!SQL_MAIN!"
    ) > "!TMPSQL!"
    
    echo Connecting to MySQL...
    mysql -h!DB_HOST! -P!DB_PORT! -u!DB_ROOT_USER! -p!DB_ROOT_PASS! < "!TMPSQL!"
    
    if errorlevel 1 (
        echo.
        echo [ERROR] Database init failed. Check:
        echo   - MySQL service is running
        echo   - Root password is correct
        echo   - You have sufficient permissions
        echo.
        echo [TIP] If permission denied, manually run: backend/db/init_windows.sql
        del "%TMPSQL%"
        pause
        exit /b 1
    )
    
    del "%TMPSQL%"
    echo.
    echo [OK] Database initialization completed!
) else (
    echo ==========================================
    echo [INFO] Database init skipped
    echo ==========================================
    echo.
    echo Please manually execute SQL files:
    echo   - Full: !SQL_MAIN!
    echo   - Windows: !SQL_WIN!
    echo.
    echo Or use command:
    echo   mysql -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -p!DB_PASS! !DB_NAME! ^<!SQL_WIN!"
)

echo.
echo ==========================================
echo          Init Completed!
echo ==========================================
echo.
echo Admin Account:
echo   Username: !ADMIN_USER!
echo   Email:    !ADMIN_EMAIL!
echo   Password: !ADMIN_PASS! (encrypted)
echo.
echo SQL files updated:
echo   - !SQL_MAIN!
echo   - !SQL_WIN!
echo.

pause
endlocal
exit /b 0
