FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .. .
RUN ./mvnw -pl delivery-agent -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash curl
WORKDIR /app
COPY --from=build /app/delivery-agent/target/*.jar app.jar
COPY --from=build /app/delivery-agent/skills/bikes/scripts/ /app/skills/bikes/scripts/
RUN chmod +x /app/skills/bikes/scripts/*.sh
EXPOSE 8089
ENV BIKES_SKILL_SCRIPTS_PATH=/app/skills/bikes/scripts
ENTRYPOINT ["java", "-jar", "app.jar"]
