FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --chown=spring:spring --from=build /app/target/B2BSystem-1.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xss512k", "-XX:MaxRAMPercentage=40.0", "-XX:MaxMetaspaceSize=160m", "-XX:ReservedCodeCacheSize=64m", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]