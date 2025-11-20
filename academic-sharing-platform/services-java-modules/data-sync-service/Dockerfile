FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
# Copy only Maven descriptor first for better caching
COPY pom.xml ./
COPY src ./src

# Build the project (skip tests for faster image build)
RUN mvn -B package -DskipTests -DskipITs

FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the fat jar produced by the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 3004
ENTRYPOINT ["java","-jar","/app/app.jar"]
