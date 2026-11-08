"""把 .env 里轮换后的数据库口令应用到运行中的 MySQL 容器。

用法（astrnest-mysql 容器须已启动）:
    python tools/apply_db_rotation.py

原理: 用旧口令（轮换前值，见 2026-11-03 审计报告）登录，把 root 与应用账号
改成 .env 中当前的新口令。新口令只在管道中传递，不落任何明文文件。
旧口令仅作为登录凭据出现在本文件常量中（该值本就是审计认定的弱口令，轮换后作废）。
"""
import re
import subprocess
import sys
from pathlib import Path

# 轮换前的旧口令（2026-11-03 审计记录：root=123456 / 应用账号=chenxi123!）
OLD_ROOT = "123456"
OLD_APP = "chenxi123!"

env_path = Path(__file__).resolve().parents[1] / ".env"


def read_key(key: str) -> str:
    m = re.search(rf"^{re.escape(key)}=(.*)$", env_path.read_text(encoding="utf-8"), re.M)
    if not m:
        sys.exit(f".env 里找不到 {key}")
    return m.group(1).strip()


def main() -> None:
    new_root = read_key("MYSQL_ROOT_PASSWORD")
    new_app = read_key("MYSQL_PASSWORD")
    app_user = read_key("MYSQL_USER")
    sql = (
        f"ALTER USER 'root'@'localhost' IDENTIFIED BY '{new_root}';"
        f"ALTER USER '{app_user}'@'%' IDENTIFIED BY '{new_app}';"
        "FLUSH PRIVILEGES;"
    )
    result = subprocess.run(
        ["docker", "exec", "-i", "astrnest-mysql", "mysql", "-uroot", f"-p{OLD_ROOT}"],
        input=sql,
        text=True,
        capture_output=True,
    )
    # docker exec 的告警(Using a password on the command line)走 stderr，可忽略
    if result.returncode != 0:
        err = result.stderr.strip().splitlines()
        sys.exit("应用失败: " + (err[-1] if err else f"exit={result.returncode}"))
    print(f"完成: root 与 {app_user} 的口令已更新为 .env 中的新值。")


if __name__ == "__main__":
    main()
