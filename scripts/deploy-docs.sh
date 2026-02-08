#!/bin/bash

# AstrNest 文档部署脚本
# 用于手动部署文档到 GitHub Pages

echo "🚀 开始部署 AstrNest 文档到 GitHub Pages..."

# 检查是否在正确的目录
if [ ! -d "AstrNest-docs" ]; then
    echo "❌ 错误：请在项目根目录运行此脚本"
    exit 1
fi

# 进入文档目录
cd AstrNest-docs

# 安装依赖
echo "📦 安装依赖..."
npm install

# 构建文档
echo "🔨 构建文档..."
npm run build

# 检查构建是否成功
if [ ! -d ".vitepress/dist" ]; then
    echo "❌ 构建失败：未找到 dist 目录"
    exit 1
fi

echo "✅ 文档构建完成！"
echo ""
echo "📋 下一步操作："
echo "1. 将 .vitepress/dist 目录内容推送到 gh-pages 分支"
echo "2. 或在 GitHub 仓库设置中启用 GitHub Pages"
echo "3. 访问 https://luminous-ChenXi.github.io/astrnest"
echo ""
echo "💡 提示：推荐使用 GitHub Actions 自动部署（已配置）"
echo "   每次推送到 main 分支时自动更新文档"