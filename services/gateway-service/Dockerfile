FROM eclipse-temurin:17-jdk-jammy
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser
WORKDIR /gateway-service
ARG JAR_FILE=target/gateway-service.jar
COPY ${JAR_FILE} app.jar
CMD ["java", "-jar", "app.jar"]