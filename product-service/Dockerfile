FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app
COPY target/product-service-0.0.1-SNAPSHOT.jar /app/product-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "product-service.jar"]
