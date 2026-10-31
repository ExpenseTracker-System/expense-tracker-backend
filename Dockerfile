# المرحلة 1: بناء المشروع مع تحديد سقف ذاكرة Maven
FROM maven:3.9.9-eclipse-temurin-23 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xmx320m"
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# المرحلة 2: تشغيل السيرفر بأقصى سرعة مع حماية سقف الذاكرة (256MB Heap)
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-XX:MaxMetaspaceSize=96m", "-XX:+UseSerialGC", "-jar", "app.jar"]