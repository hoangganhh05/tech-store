FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Cache Maven dependencies independently from application source changes.
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package


FROM eclipse-temurin:21-jre-alpine

RUN addgroup --system spring \
    && adduser --system --ingroup spring spring \
    && mkdir -p /app /data/uploads/products \
    && chown -R spring:spring /app /data

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
