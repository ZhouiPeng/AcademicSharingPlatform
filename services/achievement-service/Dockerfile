FROM eclipse-temurin:17-jdk-jammy
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser
WORKDIR /achievement-service
ARG JAR_FILE=target/achievement-service.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8002
CMD ["java", "-jar", "app.jar"]