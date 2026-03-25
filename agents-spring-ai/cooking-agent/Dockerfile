FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .. .
RUN ./mvnw -pl cooking-agent -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/cooking-agent/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
