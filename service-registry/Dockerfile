FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app
COPY target/service-registry-0.0.1-SNAPSHOT.jar /app/service-registry.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "service-registry.jar"]
