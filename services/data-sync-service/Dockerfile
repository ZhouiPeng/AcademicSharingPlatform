FROM eclipse-temurin:17-jdk-jammy
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser
WORKDIR /data-sync-service
ARG JAR_FILE=target/data-sync-service.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8086
CMD ["java", "-jar", "app.jar"]