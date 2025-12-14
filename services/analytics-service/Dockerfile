FROM eclipse-temurin:17-jdk-jammy
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser
WORKDIR /analytics-service
ARG JAR_FILE=target/analytics-service.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8006
CMD ["java", "-jar", "app.jar"]