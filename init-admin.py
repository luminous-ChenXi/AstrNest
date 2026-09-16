#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AstrNest Database Initialization Tool
数据库初始化与管理员工具

功能：
1. 自动检测操作系统（Windows/Linux/macOS）
2. 根据系统选择对应的 SQL 文件
3. 使用 root 账号创建数据库和应用用户
4. 检查应用用户是否存在，处理密码不一致情况
5. 设置管理员账号，自动 bcrypt 加密密码
6. 更新 SQL 文件并执行初始化

Windows: 更新 init_windows.sql（不包含 CREATE USER/GRANT）
Linux/macOS: 更新 init.sql（包含完整权限设置）
"""

import os
import sys
import re
import subprocess
import platform
from pathlib import Path
from getpass import getpass

# Colors for terminal output
class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_header():
    system = platform.system()
    print(f"{Colors.HEADER}{'='*60}{Colors.ENDC}")
    print(f"{Colors.BOLD}        AstrNest Database Initialization Tool{Colors.ENDC}")
    print(f"{Colors.OKCYAN}        检测到系统: {system}{Colors.ENDC}")
    print(f"{Colors.HEADER}{'='*60}{Colors.ENDC}")
    print()

def get_sql_file_path(script_dir: Path) -> tuple:
    """根据操作系统返回对应的 SQL 文件路径"""
    system = platform.system()
    
    if system == "Windows":
        sql_file = script_dir / "backend" / "db" / "init_windows.sql"
        sql_type = "windows"
        print(f"{Colors.OKCYAN}[INFO] Windows 系统 - 使用 init_windows.sql{Colors.ENDC}")
        print(f"{Colors.WARNING}      注意：Windows 版本不包含 CREATE USER/GRANT 语句{Colors.ENDC}")
    else:
        sql_file = script_dir / "backend" / "db" / "init.sql"
        sql_type = "linux"
        print(f"{Colors.OKCYAN}[INFO] Linux/macOS 系统 - 使用 init.sql{Colors.ENDC}")
        print(f"{Colors.WARNING}      注意：Linux 版本包含完整的权限设置{Colors.ENDC}")
    
    return sql_file, sql_type

def check_python():
    """Check if Python 3 is available"""
    if sys.version_info < (3, 6):
        print(f"{Colors.FAIL}[错误] 需要 Python 3.6 或更高版本{Colors.ENDC}")
        sys.exit(1)

def install_bcrypt():
    """Install bcrypt if not available"""
    try:
        import bcrypt
        return bcrypt
    except ImportError:
        print(f"{Colors.WARNING}正在安装 bcrypt...{Colors.ENDC}")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "bcrypt", "-q"])
        import bcrypt
        return bcrypt

def hash_password(password: str) -> str:
    """Hash password using bcrypt"""
    bcrypt = install_bcrypt()
    hashed = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt(rounds=12))
    return hashed.decode('utf-8')

def run_mysql_command(host: str, port: str, user: str, password: str, command: str) -> tuple:
    """Run a MySQL command and return (success, output, error)"""
    try:
        cmd = ['mysql', f'-h{host}', f'-P{port}', f'-u{user}', f'-p{password}', '-e', command]
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
        return (result.returncode == 0, result.stdout, result.stderr)
    except FileNotFoundError:
        return (False, "", "MySQL client not found")
    except Exception as e:
        return (False, "", str(e))

def check_user_exists(host: str, port: str, root_user: str, root_pass: str, target_user: str) -> tuple:
    """Check if a MySQL user exists. Returns (exists, has_password)"""
    cmd = f"SELECT COUNT(*) FROM mysql.user WHERE user='{target_user}'"
    success, stdout, stderr = run_mysql_command(host, port, root_user, root_pass, cmd)
    
    if not success:
        print(f"{Colors.FAIL}[错误] 无法查询用户: {stderr}{Colors.ENDC}")
        return (False, False)
    
    # Parse output
    lines = stdout.strip().split('\n')
    for line in lines:
        if line.strip().isdigit():
            exists = int(line.strip()) > 0
            return (exists, exists)
    
    return (False, False)

def verify_user_password(host: str, port: str, user: str, password: str) -> bool:
    """Verify if the password is correct for the user"""
    try:
        cmd = ['mysql', f'-h{host}', f'-P{port}', f'-u{user}', f'-p{password}', '-e', 'SELECT 1']
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
        return result.returncode == 0
    except:
        return False

def create_database_and_user_windows(host: str, port: str, root_user: str, root_pass: str, 
                                     db_name: str, app_user: str, app_pass: str) -> bool:
    """Create database and application user for Windows"""
    # Windows 版本：创建数据库和用户
    commands = f"""
CREATE DATABASE IF NOT EXISTS {db_name} CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS '{app_user}'@'%' IDENTIFIED BY '{app_pass}';
GRANT ALL PRIVILEGES ON {db_name}.* TO '{app_user}'@'%';
FLUSH PRIVILEGES;
"""
    success, stdout, stderr = run_mysql_command(host, port, root_user, root_pass, commands)
    if not success:
        print(f"{Colors.FAIL}[错误] 创建数据库/用户失败: {stderr}{Colors.ENDC}")
        return False
    return True

def create_database_and_user_linux(host: str, port: str, root_user: str, root_pass: str, 
                                   db_name: str, app_user: str, app_pass: str) -> bool:
    """Create database and application user for Linux/macOS"""
    # Linux 版本：SQL 文件中已包含 CREATE USER/GRANT，这里只创建数据库
    commands = f"""
CREATE DATABASE IF NOT EXISTS {db_name} CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
"""
    success, stdout, stderr = run_mysql_command(host, port, root_user, root_pass, commands)
    if not success:
        print(f"{Colors.FAIL}[错误] 创建数据库失败: {stderr}{Colors.ENDC}")
        return False
    return True

def update_user_password(host: str, port: str, root_user: str, root_pass: str, 
                         app_user: str, new_pass: str) -> bool:
    """Update user password"""
    cmd = f"ALTER USER '{app_user}'@'%' IDENTIFIED BY '{new_pass}'; FLUSH PRIVILEGES;"
    success, stdout, stderr = run_mysql_command(host, port, root_user, root_pass, cmd)
    if not success:
        print(f"{Colors.FAIL}[错误] 更新密码失败: {stderr}{Colors.ENDC}")
        return False
    return True

def execute_sql_file(host: str, port: str, user: str, password: str, db_name: str, sql_file: str) -> bool:
    """Execute SQL file"""
    try:
        cmd = ['mysql', f'-h{host}', f'-P{port}', f'-u{user}', f'-p{password}', db_name]
        with open(sql_file, 'r', encoding='utf-8') as f:
            result = subprocess.run(cmd, stdin=f, capture_output=True, text=True, encoding='utf-8', errors='ignore')
        if result.returncode != 0:
            print(f"{Colors.FAIL}[错误] SQL 执行失败: {result.stderr}{Colors.ENDC}")
            return False
        return True
    except FileNotFoundError:
        print(f"{Colors.FAIL}[错误] 找不到 MySQL 客户端{Colors.ENDC}")
        return False
    except Exception as e:
        print(f"{Colors.FAIL}[错误] 执行 SQL 失败: {e}{Colors.ENDC}")
        return False

def update_sql_file_with_admin(filepath: str, username: str, email: str, hashed_pass: str) -> bool:
    """
    Update SQL file with new admin credentials using regex matching
    使用正则匹配更新 SQL 文件中的管理员账号信息

    支持两种 SQL 文件格式：
    - init.sql (Linux/macOS): 包含 CREATE USER/GRANT 语句
    - init_windows.sql (Windows): 不包含 CREATE USER/GRANT 语句
    """
    try:
        content = Path(filepath).read_text(encoding='utf-8')

        # Escape single quotes for SQL
        u = username.replace("'", "''")
        e = email.replace("'", "''")

        # New INSERT statement - 保持与原始 SQL 文件相同的字段列表
        # 注意：users 表有 role 和 status 字段，但 INSERT 语句使用 active 字段
        new_insert = f"""INSERT INTO users (id, username, password, nickname, email, active, created_at) VALUES (1, '{u}', '{hashed_pass}', 'Admin', '{e}', 1, NOW()) ON DUPLICATE KEY UPDATE password=VALUES(password), nickname=VALUES(nickname), email=VALUES(email), active=1;"""

        # 正则匹配模式列表 - 按优先级排序
        # 支持多行、不同字段顺序、不同值的 INSERT 语句
        patterns = [
            # 模式1: 完整的 INSERT ... ON DUPLICATE KEY UPDATE 语句（最精确）
            # 匹配: INSERT INTO users (...) VALUES (...) ON DUPLICATE KEY UPDATE ...;
            (r'INSERT\s+INTO\s+users\s*\([^)]+\)\s*VALUES\s*\([^)]+\)\s*ON\s+DUPLICATE\s+KEY\s+UPDATE[^;]+;',
             re.DOTALL | re.IGNORECASE, "完整 INSERT ON DUPLICATE"),

            # 模式2: 简单的 INSERT ... VALUES 语句（没有 ON DUPLICATE KEY UPDATE）
            # 匹配: INSERT INTO users (...) VALUES (...);
            (r'INSERT\s+INTO\s+users\s*\([^)]+\)\s*VALUES\s*\([^)]+\)\s*;',
             re.DOTALL | re.IGNORECASE, "简单 INSERT VALUES"),

            # 模式3: 宽松的匹配，捕获从 INSERT 到分号的整个语句
            # 匹配: INSERT INTO users ...;
            (r'INSERT\s+INTO\s+users\s+.*?;',
             re.DOTALL | re.IGNORECASE, "宽松 INSERT"),
        ]

        matched = False
        matched_pattern_name = None
        matched_text = None

        for pattern, flags, pattern_name in patterns:
            match = re.search(pattern, content, flags)
            if match:
                matched = True
                matched_pattern_name = pattern_name
                matched_text = match.group(0)[:80] + "..." if len(match.group(0)) > 80 else match.group(0)

                # 使用正则替换 - 只替换第一个匹配
                new_content = re.sub(pattern, new_insert, content, count=1, flags=flags)

                if new_content == content:
                    print(f"{Colors.WARNING}[警告] 正则替换未产生变化，尝试下一个模式...{Colors.ENDC}")
                    matched = False
                    continue

                break

        if not matched:
            print(f"{Colors.WARNING}[警告] 在 {filepath} 中找不到匹配的 INSERT 语句{Colors.ENDC}")
            print(f"{Colors.WARNING}      将尝试在文件末尾追加管理员账号...{Colors.ENDC}")
            # 如果找不到，在文件末尾追加（在文件最后一个分号之后）
            new_content = content.rstrip() + '\n\n-- 管理员账号（由 init-admin.py 自动添加）\n' + new_insert + '\n'

        # 写入更新后的内容
        Path(filepath).write_text(new_content, encoding='utf-8')

        if matched_pattern_name:
            print(f"{Colors.OKGREEN}[OK] 已使用正则模式更新: {filepath}{Colors.ENDC}")
            print(f"{Colors.OKCYAN}      匹配模式: {matched_pattern_name}{Colors.ENDC}")
            print(f"{Colors.OKCYAN}      匹配内容: {matched_text}{Colors.ENDC}")
        else:
            print(f"{Colors.OKGREEN}[OK] 已在文件末尾追加管理员账号: {filepath}{Colors.ENDC}")

        return True

    except FileNotFoundError:
        print(f"{Colors.FAIL}[错误] 找不到文件: {filepath}{Colors.ENDC}")
        return False
    except PermissionError:
        print(f"{Colors.FAIL}[错误] 没有权限写入文件: {filepath}{Colors.ENDC}")
        return False
    except Exception as e:
        print(f"{Colors.FAIL}[错误] 更新 {filepath} 失败: {e}{Colors.ENDC}")
        import traceback
        traceback.print_exc()
        return False


def update_sql_file_with_admin_legacy(filepath: str, username: str, email: str, hashed_pass: str) -> bool:
    """
    Legacy method for updating SQL file - kept for reference
    This method uses simple string replacement without regex
    """
    try:
        content = Path(filepath).read_text(encoding='utf-8')

        # Escape single quotes
        u = username.replace("'", "''")
        e = email.replace("'", "''")

        # New INSERT statement
        new_insert = f"""INSERT INTO users (id, username, password, nickname, email, active, created_at) VALUES (1, '{u}', '{hashed_pass}', 'Admin', '{e}', 1, NOW()) ON DUPLICATE KEY UPDATE password=VALUES(password), nickname=VALUES(nickname), email=VALUES(email), active=1;"""

        # 尝试多种模式匹配
        patterns = [
            r'INSERT INTO users\s*\([^)]+\)\s*VALUES\s*\([^)]+\)\s*ON DUPLICATE KEY UPDATE[^;]+;',
            r'INSERT INTO users.*?active = 1;',
            r'INSERT INTO users[^;]+;',
        ]

        matched = False
        for pattern in patterns:
            if re.search(pattern, content, re.DOTALL | re.IGNORECASE):
                new_content = re.sub(pattern, new_insert, content, flags=re.DOTALL | re.IGNORECASE)
                matched = True
                break

        if not matched:
            print(f"{Colors.WARNING}[警告] 在 {filepath} 中找不到 INSERT 语句{Colors.ENDC}")
            return False

        Path(filepath).write_text(new_content, encoding='utf-8')
        print(f"{Colors.OKGREEN}[OK] 已更新: {filepath}{Colors.ENDC}")
        return True
    except Exception as e:
        print(f"{Colors.FAIL}[错误] 更新 {filepath} 失败: {e}{Colors.ENDC}")
        return False

def get_input(prompt: str, default: str = None, password: bool = False) -> str:
    """Get user input with default value"""
    if default:
        full_prompt = f"{prompt} [默认: {default}]: "
    else:
        full_prompt = f"{prompt}: "
    
    if password:
        value = getpass(full_prompt).strip()
    else:
        value = input(full_prompt).strip()
    
    if not value and default:
        return default
    return value

def main():
    print_header()
    check_python()
    
    # Get script directory and SQL file based on OS
    script_dir = Path(__file__).parent.absolute()
    sql_file, sql_type = get_sql_file_path(script_dir)
    
    if not sql_file.exists():
        print(f"{Colors.FAIL}[错误] 找不到 SQL 文件: {sql_file}{Colors.ENDC}")
        print(f"{Colors.WARNING}请确保 SQL 文件存在{Colors.ENDC}")
        sys.exit(1)
    
    # ========== Step 1: Root 账号配置 ==========
    print(f"\n{Colors.OKCYAN}[步骤 1/4] MySQL Root 账号配置{Colors.ENDC}")
    print(f"{Colors.WARNING}注意：需要 MySQL root 账号来创建数据库和应用用户{Colors.ENDC}")
    print()
    
    db_host = get_input("MySQL 主机地址", "localhost")
    db_port = get_input("MySQL 端口", "3306")
    root_user = get_input("Root 用户名", "root")
    root_pass = get_input("Root 密码", password=True)
    
    # 测试 root 连接
    print(f"\n{Colors.OKCYAN}正在测试 root 连接...{Colors.ENDC}")
    if not verify_user_password(db_host, db_port, root_user, root_pass):
        print(f"{Colors.FAIL}[错误] Root 账号连接失败，请检查密码{Colors.ENDC}")
        sys.exit(1)
    print(f"{Colors.OKGREEN}[OK] Root 连接成功{Colors.ENDC}")
    
    # ========== Step 2: 应用用户配置 ==========
    print(f"\n{Colors.OKCYAN}[步骤 2/4] 应用用户配置{Colors.ENDC}")
    print()
    
    db_name = get_input("数据库名称", "astrnest")
    app_user = get_input("应用用户名", "astrnest")
    
    # 检查用户是否已存在
    print(f"\n{Colors.OKCYAN}正在检查用户 '{app_user}' 是否存在...{Colors.ENDC}")
    user_exists, _ = check_user_exists(db_host, db_port, root_user, root_pass, app_user)
    
    if user_exists:
        print(f"{Colors.WARNING}[提示] 用户 '{app_user}' 已存在{Colors.ENDC}")
        print("请选择操作：")
        print("  1. 使用现有用户（需要输入正确密码）")
        print("  2. 重新设置密码（将修改现有用户密码）")
        print("  3. 退出")
        print()
        
        choice = get_input("请选择 [1/2/3]", "1")
        
        if choice == "3":
            print(f"{Colors.WARNING}已取消{Colors.ENDC}")
            sys.exit(0)
        
        app_pass = get_input(f"用户 '{app_user}' 的密码", password=True)
        
        # 验证密码
        if not verify_user_password(db_host, db_port, app_user, app_pass):
            if choice == "1":
                print(f"{Colors.FAIL}[错误] 密码不正确！{Colors.ENDC}")
                print(f"{Colors.WARNING}建议：选择选项 2 重新设置密码，或联系数据库管理员{Colors.ENDC}")
                sys.exit(1)
            elif choice == "2":
                print(f"{Colors.WARNING}正在更新用户密码...{Colors.ENDC}")
                if not update_user_password(db_host, db_port, root_user, root_pass, app_user, app_pass):
                    sys.exit(1)
                print(f"{Colors.OKGREEN}[OK] 密码已更新{Colors.ENDC}")
        else:
            print(f"{Colors.OKGREEN}[OK] 密码验证通过{Colors.ENDC}")
    else:
        print(f"{Colors.OKCYAN}用户 '{app_user}' 不存在，将创建新用户{Colors.ENDC}")
        while True:
            app_pass = get_input(f"设置用户 '{app_user}' 的密码", password=True)
            if app_pass:
                break
            print(f"{Colors.FAIL}[错误] 密码不能为空，请输入强密码{Colors.ENDC}")
    
    # 创建数据库和用户（根据系统类型）
    print(f"\n{Colors.OKCYAN}正在创建数据库和用户...{Colors.ENDC}")
    if sql_type == "windows":
        if not create_database_and_user_windows(db_host, db_port, root_user, root_pass, db_name, app_user, app_pass):
            sys.exit(1)
    else:
        if not create_database_and_user_linux(db_host, db_port, root_user, root_pass, db_name, app_user, app_pass):
            sys.exit(1)
    print(f"{Colors.OKGREEN}[OK] 数据库和用户创建完成{Colors.ENDC}")
    
    # ========== Step 3: 管理员账号配置 ==========
    print(f"\n{Colors.OKCYAN}[步骤 3/4] 管理员账号配置{Colors.ENDC}")
    print()
    
    admin_user = get_input("管理员用户名", "admin")
    admin_email = get_input("管理员邮箱", "admin@example.com")
    while True:
        admin_pass = get_input("管理员密码", password=True)
        if admin_pass:
            break
        print(f"{Colors.FAIL}[错误] 管理员密码不能为空，请输入强密码{Colors.ENDC}")
    admin_pass2 = get_input("确认密码", password=True)
    
    if admin_pass != admin_pass2:
        print(f"{Colors.FAIL}[错误] 两次输入的密码不一致！{Colors.ENDC}")
        sys.exit(1)
    
    # 加密密码
    print(f"\n{Colors.OKCYAN}正在加密密码...{Colors.ENDC}")
    hashed_pass = hash_password(admin_pass)
    print(f"{Colors.OKGREEN}[OK] 密码已加密{Colors.ENDC}")
    
    # 更新 SQL 文件
    print(f"\n{Colors.OKCYAN}正在更新 SQL 文件...{Colors.ENDC}")
    update_sql_file_with_admin(str(sql_file), admin_user, admin_email, hashed_pass)
    
    # ========== Step 4: 执行 SQL ==========
    print(f"\n{Colors.OKCYAN}[步骤 4/4] 执行 SQL 初始化...{Colors.ENDC}")
    
    # 询问使用哪个用户执行 SQL
    print()
    print("选择执行 SQL 的用户：")
    print(f"  1. 应用用户 '{app_user}'（更安全，但可能缺少某些权限）")
    print(f"  2. Root 用户 '{root_user}'（有完整权限，推荐）")
    print()
    exec_choice = get_input("请选择 [1/2]", "2")
    
    if exec_choice == "1":
        print(f"{Colors.WARNING}使用用户 '{app_user}' 执行 SQL...{Colors.ENDC}")
        sql_user, sql_pass = app_user, app_pass
    else:
        print(f"{Colors.WARNING}使用 root 用户执行 SQL...{Colors.ENDC}")
        sql_user, sql_pass = root_user, root_pass
    
    if not execute_sql_file(db_host, db_port, sql_user, sql_pass, db_name, str(sql_file)):
        print(f"\n{Colors.FAIL}[错误] SQL 执行失败！{Colors.ENDC}")
        print(f"{Colors.WARNING}可能原因：{Colors.ENDC}")
        print(f"  1. 用户缺少某些权限（如 CREATE VIEW、CREATE TRIGGER）")
        print(f"  2. SQL 文件中的某些操作需要更高权限")
        print()
        print(f"{Colors.WARNING}建议手动执行：{Colors.ENDC}")
        print(f"  mysql -h{db_host} -P{db_port} -u{root_user} -p {db_name} < {sql_file}")
        print()
        sys.exit(1)
    
    print(f"{Colors.OKGREEN}[OK] SQL 执行成功{Colors.ENDC}")
    
    # ========== 完成 ==========
    print()
    print(f"{Colors.HEADER}{'='*60}{Colors.ENDC}")
    print(f"{Colors.OKGREEN}        初始化完成！{Colors.ENDC}")
    print(f"{Colors.HEADER}{'='*60}{Colors.ENDC}")
    print()
    print(f"{Colors.BOLD}数据库信息：{Colors.ENDC}")
    print(f"  数据库名: {db_name}")
    print(f"  应用用户: {app_user}")
    print()
    print(f"{Colors.BOLD}管理员账号：{Colors.ENDC}")
    print(f"  用户名: {admin_user}")
    print(f"  邮箱:   {admin_email}")
    print(f"  密码:   {'*' * len(admin_pass)}")
    print()
    print(f"{Colors.BOLD}后端配置（application.yml）：{Colors.ENDC}")
    print(f"  spring.datasource.url: jdbc:mysql://{db_host}:{db_port}/{db_name}")
    print(f"  spring.datasource.username: {app_user}")
    print(f"  spring.datasource.password: {app_pass}")
    print()
    
    input("按回车键退出...")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(f"\n{Colors.WARNING}\n[已取消]{Colors.ENDC}")
        sys.exit(0)
    except Exception as e:
        print(f"\n{Colors.FAIL}[错误] {e}{Colors.ENDC}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
