# 多阶段构建：构建阶段
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# 复制pom文件并下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jre

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

# 复制构建产物（通配 jar 名，版本号变更不再破坏构建）
COPY --from=build /app/target/backend-*.jar app.jar

# 创建非root用户
# application-prod.yml 将日志写入 /var/log/astrnest/application.log，
# 需在构建时创建该目录并交给 spring 用户，否则容器内写日志失败
RUN groupadd -r spring && useradd -r -g spring spring \
  && mkdir -p /var/log/astrnest \
  && chown -R spring:spring /app /var/log/astrnest
USER spring

# 暴露端口
EXPOSE 8081

# 健康检查（start-period 覆盖 JVM + Spring Boot 冷启动，5s 会把正常慢启动误判为 unhealthy）
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
