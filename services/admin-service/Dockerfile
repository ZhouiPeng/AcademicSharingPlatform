FROM eclipse-temurin:17-jdk-jammy
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser
WORKDIR /admin-service
ARG JAR_FILE=target/admin-service.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8085
CMD ["java", "-jar", "app.jar"]