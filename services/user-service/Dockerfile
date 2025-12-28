FROM eclipse-temurin:17-jdk-jammy
RUN apt-get update && apt-get install -y netcat-traditional && rm -rf /var/lib/apt/lists/*
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /user-service
ARG JAR_FILE=target/user-service.jar
COPY ${JAR_FILE} app.jar
COPY docker-entrypoint.sh /user-service/
RUN chmod +x /user-service/docker-entrypoint.sh
RUN mkdir -p /user-service/logs && chown -R appuser:appgroup /user-service/logs
USER appuser
EXPOSE 8081
ENTRYPOINT ["/user-service/docker-entrypoint.sh"]