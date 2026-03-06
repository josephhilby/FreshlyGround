FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

RUN chmod +x gradlew

EXPOSE 7070

ENTRYPOINT ["./gradlew", "runServer", "--no-daemon"]