# AstrNest Backend

AstrNest 后端服务 - 基于 Spring Boot 3.4.1 的现代化媒体管理 API 服务。

## 项目简介

AstrNest Backend 是 [AstrNest](https://github.com/luminous-ChenXi/AstrNest) 项目的后端部分，提供完整的 RESTful API、用户认证、内容审查、API 密钥管理等功能。

## 核心特性

- **现代化架构**: Spring Boot 3.4.1 + Java 21
- **多角色权限**: 支持管理员、普通用户、游客等多级权限管理
- **多云存储**: 内置 Local、阿里云 OSS、腾讯 COS、七牛 Kodo、华为 OBS、金山 KS3、又拍云 USS、OneDrive/SharePoint 以及通用 S3 兼容存储
- **智能内容审查**: 接入腾讯云数据万象（COS CI）图片审核与标签服务
- **完整的API生态**: RESTful API + API密钥认证
- **安全管理**: Spring Security 6 + JWT认证
- **成员治理与配额**: 支持查看和调整用户配额、角色管理
- **邮件服务**: 集成邮件模板与验证码发送功能

## 技术栈

- **框架**: Spring Boot 3.4.1
- **语言**: Java 21
- **数据库**: MySQL 5.7+/8.0
- **安全**: Spring Security 6
- **文档**: SpringDoc OpenAPI 3
- **AI 审核 SDK**: Tencent Cloud COS CI (`com.qcloud:cos_api`)
- **构建**: Maven Wrapper

## 快速开始

### 环境要求

- Java 21+
- MySQL 5.7+ 或 8.0
- Maven 3.8+ (或使用项目自带的 Maven Wrapper)

### 1. 克隆项目

```bash
git clone https://github.com/luminous-ChenXi/AstrNest.git
cd AstrNest/backend
```

### 2. 初始化数据库

```bash
mysql -u root -p < db/init.sql
```

### 3. 配置环境

复制 `src/main/resources/application.yml` 并根据需要修改配置，包括：
- 数据库连接信息
- 邮件服务器配置
- 存储策略配置
- JWT 密钥

### 4. 启动服务

使用 Maven Wrapper:
```bash
./mvnw spring-boot:run
```

或使用系统 Maven:
```bash
mvn spring-boot:run
```

### 5. 访问 API 文档

启动后访问：http://localhost:8080/swagger-ui/index.html

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/chenxi/astrnest/
│   │   │   ├── config/          # 配置类
│   │   │   ├── controller/      # API 控制器
│   │   │   ├── service/         # 业务逻辑层
│   │   │   ├── repository/      # 数据访问层
│   │   │   ├── entity/          # 实体类
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── security/        # 安全配置
│   │   │   ├── storage/         # 存储策略实现
│   │   │   └── AstrNestApplication.java
│   │   └── resources/
│   │       ├── application.yml  # 主配置文件
│   │       └── db/              # 数据库脚本
│   └── test/                    # 测试代码
├── pom.xml                      # Maven 配置
└── mvnw/mvnw.cmd               # Maven Wrapper
```

## 配置说明

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/astrnest?useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

### 存储配置

支持多种存储策略，可在 `application.yml` 中配置：

```yaml
astrnest:
  storage:
    type: local  # 可选: local, aliyun-oss, tencent-cos, qiniu-kodo, huawei-obs, ks3, upyun, s3
    local:
      upload-path: ./storage/uploads
      public-base-url: http://localhost:8080
```

### 邮件配置

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-email@example.com
    password: your-password
```

## API 认证

### JWT Token 认证

登录后获取 Token，在请求头中添加：
```
Authorization: Bearer <your-jwt-token>
```

### API Key 认证

部分接口支持 API Key 认证：
```
X-API-Key: <your-api-key>
```

## 部署

### Docker 部署

```bash
# 构建镜像
./mvnw clean package
 docker build -t astrnest-backend .

# 运行容器
docker run -p 8080:8080 -v ./storage:/app/storage astrnest-backend
```

### 传统部署

```bash
# 打包
./mvnw clean package

# 运行
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## 开发指南

### 运行测试

```bash
./mvnw test
```

### 代码风格

项目使用默认的 Spring Boot 代码风格，建议使用 IntelliJ IDEA 进行开发。

## 相关项目

- [AstrNest](https://github.com/luminous-ChenXi/AstrNest) - 完整的 AstrNest 项目（前后端整合）
- [AstrNest Frontend](https://github.com/luminous-ChenXi/AstrNest/tree/main/frontend) - 前端项目

## 许可证

本项目基于 [GNU General Public License v3 (GPL v3)](../LICENSE) 开源。

## 联系方式

- **问题反馈**: [GitHub Issues](https://github.com/luminous-ChenXi/AstrNest/issues)
- **邮箱**: chenxi@luminouschenxi.net
- **Discord**: [LuminousChenxi](https://discord.gg/hBsqcfwC9Q)

---

⭐ 如果这个项目对您有帮助，请给我们一个 Star！
