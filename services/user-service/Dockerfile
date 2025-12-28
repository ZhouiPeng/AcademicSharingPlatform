FROM eclipse-temurin:17-jdk-jammy
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser
WORKDIR /user-service
ARG JAR_FILE=target/user-service.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]