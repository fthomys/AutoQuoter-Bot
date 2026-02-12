FROM gradle:9.3-jdk21 AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x gradlew
COPY . .
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV BOT_DATA_DIR=/app
COPY --from=build /app/build/libs/AutoQuoter.jar /app/AutoQuoter.jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "AutoQuoter.jar"]