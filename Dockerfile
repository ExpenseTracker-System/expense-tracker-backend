# المرحلة 1: بناء المشروع
FROM maven:3.9.9-eclipse-temurin-23 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xmx320m"
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# المرحلة 2: تشغيل السيرفر بدون قيد Metaspace الخانق مع ضبط الرام لـ Render
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms64m", "-Xmx220m", "-Xss512k", "-XX:+UseSerialGC", "-jar", "app.jar"]