# 贡献指南

感谢您对 AstrNest 项目的关注！我们欢迎各种形式的贡献，包括但不限于代码提交、文档改进、问题报告等。

## 开发环境设置

### 前置要求
- JDK 21+
- Node.js 18+
- MySQL 5.7+/8.0
- Git

### 本地开发设置

1. **Fork 项目**
   ```bash
   # Fork 项目到您的 GitHub 账户
   # 然后克隆到本地
   git clone https://github.com/your-username/astrnest.git
   cd astrnest
   ```

2. **设置上游仓库**
   ```bash
   git remote add upstream https://github.com/original-owner/astrnest.git
   ```

3. **数据库设置**
   ```bash
   # 创建数据库
   mysql -u root -p < backend/db/init.sql
   ```

4. **后端开发**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

5. **前端开发**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

### AI 审核密钥（可选）
- 若需在本地调试腾讯云图片审核/标签流程，请在 `.env` 或 shell 中设置以下环境变量，后端会在系统配置未填写时读取这些值：
  - `ASTRNEST_AI_TENCENT_SECRET_ID`
  - `ASTRNEST_AI_TENCENT_SECRET_KEY`
  - `ASTRNEST_AI_TENCENT_REGION`
  - `ASTRNEST_AI_TENCENT_BUCKET`
  - `ASTRNEST_AI_TENCENT_DETECT_SCENES`
  - `ASTRNEST_AI_BLOCK_CONFIDENCE` / `ASTRNEST_AI_REVIEW_CONFIDENCE` / `ASTRNEST_AI_LABEL_MIN_CONFIDENCE`
- 生产环境可通过 `.env` + Docker Compose 注入上述变量，或在后台「系统配置 > AI 智能审核」中填写。
- 新的 `aiReview` API 字段会在上传接口与用户门户响应中暴露 AI 决策/标签/RequestId，前端的 `UploadResultModal` 会直接读取该段数据并展示 Element Plus 提示，调试时可在浏览器 Network 面板观察。

## 代码提交规范

### 分支管理
- `main`: 主分支，稳定版本
- `develop`: 开发分支
- `feature/*`: 功能分支
- `bugfix/*`: 修复分支
- `hotfix/*`: 紧急修复分支

### 提交信息格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型 (type):**
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具变动

**示例:**
```
feat(upload): 添加图片批量上传功能

- 支持多文件选择
- 添加进度条显示
- 优化上传性能

Closes #123
```

## 代码规范

### 后端代码规范
- 遵循 Java 编码规范
- 使用 Lombok 减少样板代码
- 方法命名使用驼峰式
- 类名使用大驼峰式
- 常量使用全大写加下划线

### 前端代码规范
- 使用 ESLint + Prettier
- Vue 组件使用组合式 API
- CSS 使用 Tailwind CSS
- 变量命名使用驼峰式

### 数据库规范
- 表名使用小写加下划线
- 字段名使用小写加下划线
- 主键使用 `id`
- 时间字段使用 `_at` 后缀

## 测试要求

### 后端测试
```bash
cd backend
./mvnw test
```

### 前端测试
```bash
cd frontend
npm run test
```

## Pull Request 流程

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 问题报告

请使用 GitHub Issues 报告问题，并包含以下信息：
- 问题描述
- 重现步骤
- 期望行为
- 实际行为
- 环境信息
- 相关截图

## 沟通渠道

- GitHub Issues: 功能请求和问题报告
- GitHub Discussions: 技术讨论和问题咨询
- Pull Requests: 代码贡献

## 许可证

通过向本项目提交代码，您同意您的贡献将根据项目的 MIT 许可证进行授权。

---

再次感谢您的贡献！🎉