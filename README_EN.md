# AstrNest Media Management System
<div>

A modern full-stack media management platform built with Spring Boot 3.4.1 and Vue 3. Features multi-cloud storage, AI content moderation, and a complete API ecosystem.

<p align="center">
  <a href="https://github.com/vuejs/core">
    <img src="https://img.shields.io/badge/vue-3.5.24-brightgreen.svg?style=flat-square&logo=vue.js" alt="vue">
  </a>
  <a href="https://github.com/element-plus/element-plus">
    <img src="https://img.shields.io/badge/element--plus-2.8.6-brightgreen.svg?style=flat-square&logo=element" alt="element-plus">
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/spring--boot-3.4.1-brightgreen.svg?style=flat-square&logo=spring" alt="spring-boot">
  </a>
  <a href="https://github.com/luminous-ChenXi/astrnest/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/license-GPL%20v3-blue.svg?style=flat-square" alt="license">
  </a>
  <a href="https://github.com/luminous-ChenXi/astrnest/releases">
    <img src="https://img.shields.io/github/release/luminous-ChenXi/astrnest.svg?style=flat-square" alt="GitHub release">
  </a>
  <a href="https://coderabbit.ai">
    <img src="https://img.shields.io/coderabbit/prs/github/luminous-ChenXi/AstrNest?labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews" alt="CodeRabbit Reviews">
  </a>
</p>
<p align="center">
  <a href="./README.md">简体中文</a> · <a href="./README_EN.md">English</a>
</p>
<p align="center">
  <a href="#quick-start">Quick Start</a> · 
  <a href="https://luminouschenxi.com">Blog</a> · 
  <a href="#tech-stack">Tech Stack</a> · 
  <a href="#acknowledgments">Acknowledgments</a> ·   
  <a href="https://discord.gg/hBsqcfwC9Q">Discord</a> · 
  <a href="https://github.com/luminous-ChenXi/astrnest">GitHub</a>
</p>

</div>

<img src="https://cdn.luminouschenxi.net/assets/others/templates/1728x2304.png" width = "300" height = "400" alt="AstrNest" align=right />
<div align="center">

# AstrNest

_Modern full-stack image hosting platform built with Spring Boot 3.4.1 and Vue 3._

Youth and dreams are the only things not to be let down!

</div>

---

## Core Features
- **Modern Architecture**: Full-stack tech stack with Spring Boot 3.4.1 + Vue 3 + Vite 5
- **Multi-role Permissions**: Supports multi-level permission management system for administrators, regular users, etc.
- **Multi-cloud Storage**: Built-in drivers for Local, Alibaba Cloud OSS, Tencent Cloud COS, Qiniu Kodo, Huawei OBS, Kingsoft KS3, UpYun USS, OneDrive/SharePoint, and generic S3-compatible storage; one-click switch in configuration. S3-compatible drivers trigger 25MB multipart upload when exceeding 5GB threshold, supporting CDN/CNAME, accelerated domains, PathStyle, and pre-signed direct upload credentials.
- **Intelligent Content Moderation**: Integrates Tencent Cloud COS CI (Cloud Infinite) image moderation and tagging services, automatically populates AI decisions/tags/RequestId, outputs user-friendly prompts combined with error code documentation, and provides double protection with manual review.
- **Complete API Ecosystem**: RESTful API + API key authentication + Web management interface
- **Flexible Storage Solutions**: Supports local storage and cloud object storage (OSS/COS)
- **Security Management**: Spring Security 6 + JWT authentication + Content Security Policy (CSP)
- **Member Governance & Quotas**: Admin "Member List" supports viewing avatars/quotas/total likes, and allows one-click adjustment of daily upload & total storage limits, as well as switching roles (Admin/User/Guest)
- **Responsive Design**: Modern UI component library supporting multi-device adaptation (PC, mobile, tablet, etc.)
- **Real-time Monitoring**: System runtime status monitoring and operation log auditing
- **Email Service**: Integrates email templates and verification code sending functionality, with Aliyun Post Office SMTP preconfigured by default.

<p align="center">
  <b>🎨 Theme Preview</b>
</p>

<table align="center">
  <tr>
    <td align="center" width="50%">
      <img src="./templates/index.png" width="100%" alt="User Frontend" />
      <br>
      <sub><b>User Frontend</b></sub>
    </td>
    <td align="center" width="50%">
      <img src="./templates/managerIndex.png" width="100%" alt="Admin Frontend" />
      <br>
      <sub><b>Admin Frontend</b></sub>
    </td>
  </tr>
</table>

## License Warning | GPL v3 Open Source License Notice
### ⚠️ **Important License Reminder** ⚠️

**This project is open-sourced under the GNU General Public License v3 (GPL v3) license**

### Legal Statement:
- **Any use, modification, or distribution of this code must comply with the GPL v3 license**
- **Derivative works based on this project must also be open-sourced under the GPL v3 license**
- **Prohibited to use this code in closed-source commercial projects**
- **Prohibited to remove copyright information and license statements**

### Warning to Non-Compliant Developers:
**Note: The following actions constitute infringement and may result in legal liability:**
- ❌ Modifying the license or removing copyright statements without permission
- ❌ Using the code in closed-source commercial products without open-sourcing derivatives
- ❌ Claiming the code as original work
- ❌ Circumventing GPL v3 requirements to distribute derivative works

### Your Obligations:
- ✅ Retain original copyright and license information  
- ✅ Make modifications based on this project open-source as well  
- ✅ Provide source code when distributing  
- ✅ Clearly identify modified content and modifiers

**This project took 3 months of development with significant effort by the ChenXi Team. Violating the GPL v3 license will result in legal liability. Please respect the spirit of open source!**

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.4.1
- **Language**: Java 21
- **Database**: MySQL 5.7+/8.0
- **Security**: Spring Security 6
- **Documentation**: SpringDoc OpenAPI 3
- **AI Moderation SDK**: Tencent Cloud COS CI (`com.qcloud:cos_api`)
- **Build Tool**: Maven Wrapper

### Frontend
- **Framework**: Vue 3.5.24
- **Build Tool**: Vite 5.4.10
- **Routing**: Vue Router 4
- **State Management**: Pinia 3
- **UI Components**: Element Plus 2.8.6
- **Styling**: Tailwind CSS 3
- **Icons**: Lucide Vue, Element Plus Icons

## System Architecture

### Architecture Diagram
AstrNest adopts a decoupled front-end and back-end architecture:

- **Frontend**: Vue 3 + Vite + Element Plus + Tailwind CSS
- **Backend**: Spring Boot 3 + Spring Security + JPA + MySQL
- **Authentication**: Dual authentication mechanism (JWT Token + API Key)
- **Storage**: Local storage + extended support for cloud object storage
- **Security**: Role-Based Access Control (RBAC) + Content Security Policy

### Project Structure

```
astrnest/
├─ backend/      # Spring Boot service: REST API, authentication, content moderation, API key management
├─ frontend/     # Vue 3 + Vite single-page application: dashboard, upload center, security console, API integration
└─ storage/      # (Runtime-generated) Local storage directory, configurable to OSS/COS
```

## API Documentation Overview

### Swagger / OpenAPI
- Online documentation entry: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
- Authentication method: Place the Token obtained after login in `Authorization: Bearer <token>`. Some interfaces require administrator privileges.

## Quick Start (Development)

Taking local development on Ubuntu as an example:

1) Clone & Install Dependencies
```bash
git clone https://github.com/luminous-ChenXi/AstrNest.git
cd AstrNest
```
2) Initialize Database (Local MySQL)
```bash
mysql -u root -p < backend/db/init.sql
```
3) Start Backend
```bash
cd backend
./mvnw spring-boot:run
```
4) Start Frontend (New Terminal)
```bash
cd frontend
npm install
npm run dev
```
5) Access
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- API Documentation: http://localhost:8080/swagger-ui/index.html

**Default Administrator**: `admin` / `chenxi123` (Please change immediately).

> For detailed configuration (environment variables, file paths, initialization scripts, FFmpeg, storage switching, etc.), refer to `CONFIG_GUIDE.md`.

### Deployment Overview
- **Docker Compose (Recommended)**: Copy `.env.example` → `.env`, fill in database/domain/SMTP/storage information, then execute:
```bash
docker compose --env-file .env up -d
```
- **Traditional Deployment**: Package backend with `./mvnw clean package && java -jar target/backend-0.0.1-SNAPSHOT.jar`; build frontend with `npm run build` and deploy the `dist/` directory to Nginx/CDN.

For more detailed environment variables, Nginx reverse proxy, CDN/object storage switching, see `CONFIG_GUIDE.md`.

## Problem Solving

| Issue | Recommended Solution |
| --- | --- |
| CORS Error | Ensure the CORS whitelist in backend `SecurityConfig` includes the current frontend domain, or add `Access-Control-*` headers at the deployment entry layer (Nginx). |
| Database Authentication Failure | Check if `spring.datasource.username/password` matches MySQL user authorization. |
| API Upload 401 | `POST /api/uploads` requires logging in as an administrator or carrying a valid `X-API-Key`. Create a key in the Security Center/API page. |
| Static Resource Access Failure | If switching to object storage, remember to synchronize the configuration of `astrnest.storage.local.public-base-url` or set the asset domain in the background, and ensure CDN/bucket permissions are correct. |
| Email Sending Failure | Check if email configuration is correct, including SMTP server, port, username, and password |
| Verification Code Validation Failure | Ensure the verification code is within the validity period and entered correctly |

### Common Scripts
- Backend Development: `cd backend && ./mvnw spring-boot:run`
- Backend Testing: `cd backend && ./mvnw test`
- Frontend Development: `cd frontend && npm run dev`
- Frontend Build: `cd frontend && npm run build`

### API Quick Examples
```bash
# Login
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"chenxi123"}'

# Upload (API Key)
curl -X POST "http://localhost:8080/api/uploads" \
  -H "X-API-Key: <your-api-key>" \
  -F "file=@/path/to/image.jpg"
```

## Development Roadmap
- ~~Add manual image moderation feature to support manual review of image violations~~ ✅
- ~~Add AI-based image violation query feature to support batch query of image violations~~ ✅
- ~~Improve image Tag feature to support batch addition, deletion, and search~~ ✅
- ~~Support more cloud storage providers (Alibaba Cloud OSS, Tencent Cloud COS, etc.)~~ ✅
- [ ] Code beginner-friendly initialization page
- [ ] Image compression and format conversion features
- [ ] Image watermarking feature
- [ ] Image batch processing tools
- [ ] Mobile application development
- [ ] Third-party login integration

## Acknowledgments

### Open Source Component Compliance Statement
This project is built based on numerous excellent open-source components. Thanks to the contributions of the open-source community! The following lists major dependent components and their license information by frontend/backend for compliance checking:

#### Frontend (Refer to /frontend/package.json)
- **Vue 3** [`vue@^3.5.24`](https://github.com/vuejs/core) - [MIT License](https://github.com/vuejs/core/blob/main/LICENSE)
- **Vue Router** [`vue-router@^4.6.3`](https://github.com/vuejs/router) - [MIT License](https://github.com/vuejs/router/blob/main/LICENSE)
- **Pinia** [`pinia@^3.0.4`](https://github.com/vuejs/pinia) - [MIT License](https://github.com/vuejs/pinia/blob/main/LICENSE)
- **Element Plus** [`element-plus@^2.8.6`](https://github.com/element-plus/element-plus) - [MIT License](https://github.com/element-plus/element-plus/blob/dev/LICENSE)
- **Element Plus Icons** [`@element-plus/icons-vue@^2.3.1`](https://github.com/element-plus/element-plus-icons) - [MIT License](https://github.com/element-plus/element-plus-icons/blob/main/LICENSE)
- **Vite** [`vite@^5.4.10`](https://github.com/vitejs/vite) - [MIT License](https://github.com/vitejs/vite/blob/main/LICENSE)
- **Vue Plugin** [`@vitejs/plugin-vue@^5.1.4`](https://github.com/vitejs/vite-plugin-vue) - [MIT License](https://github.com/vitejs/vite-plugin-vue/blob/main/LICENSE)
- **Tailwind CSS** [`tailwindcss@^3.4.17`](https://github.com/tailwindlabs/tailwindcss) - [MIT License](https://github.com/tailwindlabs/tailwindcss/blob/master/LICENSE)
- **Autoprefixer** [`autoprefixer@^10.4.22`](https://github.com/postcss/autoprefixer) - [MIT License](https://github.com/postcss/autoprefixer/blob/main/LICENSE)
- **PostCSS** [`postcss@^8.5.6`](https://github.com/postcss/postcss) - [MIT License](https://github.com/postcss/postcss/blob/main/LICENSE)
- **Axios** [`axios@^1.7.7`](https://github.com/axios/axios) - [MIT License](https://github.com/axios/axios/blob/main/LICENSE)
- **Day.js** [`dayjs@^1.11.13`](https://github.com/iamkun/dayjs) - [MIT License](https://github.com/iamkun/dayjs/blob/dev/LICENSE)
- **DOMPurify** [`dompurify@^3.3.0`](https://github.com/cure53/DOMPurify) - [Apache License 2.0](https://github.com/cure53/DOMPurify/blob/main/LICENSE)
- **Marked** [`marked@^12.0.2`](https://github.com/markedjs/marked) - [MIT License](https://github.com/markedjs/marked/blob/master/LICENSE)
- **GSAP** [`gsap@^3.12.5`](https://github.com/greensock/GSAP) - [Standard 'No Charge' License](https://github.com/greensock/GSAP/blob/master/LICENSE)
- **Lucide Icons** [`lucide-vue-next@^0.555.0`](https://github.com/lucide-icons/lucide) - [ISC License](https://github.com/lucide-icons/lucide/blob/main/LICENSE)
- **Plyr** [`plyr`](https://github.com/sampotts/plyr) - [MIT License](https://github.com/sampotts/plyr/blob/master/LICENSE.md)

#### Backend (Refer to /backend/pom.xml)
- **Spring Boot 3.4.1** - [Apache License 2.0](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)
  - spring-boot-starter-web
  - spring-boot-starter-security  
  - spring-boot-starter-data-jpa
  - spring-boot-starter-validation
  - spring-boot-starter-mail
  - spring-boot-starter-actuator
  - spring-boot-starter-test
- **SpringDoc OpenAPI** [`springdoc-openapi-starter-webmvc-ui@2.7.0`](https://github.com/springdoc/springdoc-openapi) - [Apache License 2.0](https://github.com/springdoc/springdoc-openapi/blob/master/LICENSE)
- **Jackson** [`jackson-datatype-jsr310`](https://github.com/FasterXML/jackson) - [Apache License 2.0](https://github.com/FasterXML/jackson/blob/master/LICENSE)
- **MySQL Connector/J** - [GPL License with FOSS License Exception](https://github.com/mysql/mysql-connector-j/blob/release/8.x/LICENSE)
- **AWS SDK S3** [`software.amazon.awssdk:s3@2.25.58`](https://github.com/aws/aws-sdk-java-v2) - [Apache License 2.0](https://github.com/aws/aws-sdk-java-v2/blob/master/LICENSE.txt)
- **Alibaba Cloud OSS SDK** [`com.aliyun.oss:aliyun-sdk-oss@3.17.4`](https://github.com/aliyun/aliyun-oss-java-sdk) - [Apache License 2.0](https://github.com/aliyun/aliyun-oss-java-sdk/blob/master/LICENSE)
- **Tencent Cloud COS SDK** [`com.qcloud:cos_api@5.6.255.1`](https://github.com/tencentyun/cos-java-sdk-v5) - [MIT License](https://github.com/tencentyun/cos-java-sdk-v5/blob/master/LICENSE)
- **UpYun Java SDK** [`com.upyun:java-sdk@4.2.3`](https://github.com/upyun/java-sdk) - [MIT License](https://github.com/upyun/java-sdk/blob/master/LICENSE)
- **Lombok** - [MIT License](https://github.com/projectlombok/lombok/blob/master/LICENSE)
- **Spring Boot Configuration Processor** - [Apache License 2.0](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)
- **H2 Database** (test) - [MPL 2.0 / EPL 1.0](https://github.com/h2database/h2database/blob/master/LICENSE.txt)
- **Spring Security Test** (test) - [Apache License 2.0](https://github.com/spring-projects/spring-security/blob/main/LICENSE.txt)

#### Disclaimer
- The above license information is based on the latest information from each component's official repository
- Specific versions may change as the project is updated
- Please ensure compliance with all dependent components' license requirements when using this project
- It is recommended to conduct a detailed license compliance review before commercial use


## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for detailed processes.

## Issue Reporting

If you encounter issues:
1. Check [Frequently Asked Questions](#problem-solving)
2. Search [GitHub Issues](https://github.com/luminous-ChenXi/astrnest/issues)
3. Create a new Issue (your valuable suggestions are also welcome!)
4. Check [Contact](#contact)

## License

This project is open-sourced under the [GNU General Public License v3 (GPL v3)](LICENSE).

![GPL-v3](https://www.gnu.org/graphics/gplv3-127x51.png)

## Security

For security-related issues, please refer to [SECURITY.md](SECURITY.md).


## Contact

- **Issue Feedback**: Submit via GitHub Issues
- **Email**: chenxi@luminouschenxi.net
- **Discord**: [LuminousChenxi](https://discord.gg/hBsqcfwC9Q)

---
Please star! Follow! Support!
⭐ If this project helps you, please give us a Star!