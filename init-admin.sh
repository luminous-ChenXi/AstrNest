#!/usr/bin/env bash
# AstrNest Database Initialization Tool for Linux/macOS
# 数据库初始化与管理员工具（Linux/macOS 版本）
# 支持 Ubuntu 24+ 虚拟环境自动创建

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$SCRIPT_DIR/.venv"

print_header() {
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${BOLD}        AstrNest Database Initialization Tool${NC}"
    echo -e "${CYAN}        检测到系统: Linux/macOS${NC}"
    echo -e "${BLUE}============================================================${NC}"
    echo
}

check_python() {
    if command -v python3 &> /dev/null; then
        PYTHON_CMD="python3"
    elif command -v python &> /dev/null; then
        PYTHON_CMD="python"
    else
        echo -e "${RED}[错误] 未找到 Python 3${NC}"
        echo "请安装 Python 3:"
        echo "  Ubuntu/Debian: sudo apt-get install python3 python3-venv python3-pip"
        echo "  CentOS/RHEL:   sudo yum install python3"
        echo "  macOS:         brew install python3"
        exit 1
    fi

    # Check version
    PY_VERSION=$($PYTHON_CMD -c 'import sys; print(sys.version_info[:2])' | tr -d '(),')
    PY_MAJOR=$(echo $PY_VERSION | cut -d' ' -f1)
    PY_MINOR=$(echo $PY_VERSION | cut -d' ' -f2)

    if [ "$PY_MAJOR" -lt 3 ] || ([ "$PY_MAJOR" -eq 3 ] && [ "$PY_MINOR" -lt 6 ]); then
        echo -e "${RED}[错误] 需要 Python 3.6 或更高版本${NC}"
        exit 1
    fi

    echo -e "${GREEN}[OK] Python 版本: $PY_MAJOR.$PY_MINOR${NC}"
}

check_ubuntu_version() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        if [ "$ID" = "ubuntu" ]; then
            UBUNTU_VERSION=${VERSION_ID%%.*}
            if [ "$UBUNTU_VERSION" -ge 24 ]; then
                echo -e "${YELLOW}[INFO] 检测到 Ubuntu $VERSION_ID，将使用虚拟环境${NC}"
                return 0
            fi
        fi
    fi
    return 1
}

setup_venv() {
    echo -e "${CYAN}[步骤] 设置 Python 虚拟环境...${NC}"

    if [ ! -d "$VENV_DIR" ]; then
        echo "创建虚拟环境..."
        $PYTHON_CMD -m venv "$VENV_DIR"
    fi

    # Activate venv
    source "$VENV_DIR/bin/activate"

    # Upgrade pip
    pip install --upgrade pip -q

    # Install bcrypt
    if ! python -c "import bcrypt" 2>/dev/null; then
        echo "安装 bcrypt..."
        pip install bcrypt -q
    fi

    echo -e "${GREEN}[OK] 虚拟环境准备完成${NC}"
}

run_python_script() {
    if [ -d "$VENV_DIR" ]; then
        # Use venv python
        "$VENV_DIR/bin/python" "$SCRIPT_DIR/init-admin.py"
    else
        # Use system python
        $PYTHON_CMD "$SCRIPT_DIR/init-admin.py"
    fi
}

main() {
    print_header
    check_python

    # Check if Ubuntu 24+ and setup venv if needed
    if check_ubuntu_version; then
        setup_venv
    else
        echo -e "${CYAN}[INFO] 使用系统 Python${NC}"
        # Try to install bcrypt if not present
        if ! $PYTHON_CMD -c "import bcrypt" 2>/dev/null; then
            echo -e "${YELLOW}[INFO] 安装 bcrypt...${NC}"
            $PYTHON_CMD -m pip install bcrypt -q
        fi
    fi

    echo
    echo -e "${CYAN}[INFO] 启动初始化脚本...${NC}"
    echo

    # Run the Python script
    run_python_script
}

# Run main
main
