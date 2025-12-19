FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app
COPY target/media-service-0.0.1-SNAPSHOT.jar /app/media-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "media-service.jar"]
