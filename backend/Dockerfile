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

# 复制构建产物
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# 创建非root用户
RUN groupadd -r spring && useradd -r -g spring spring
RUN chown -R spring:spring /app
USER spring

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
