# Stage 1: Build
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=builder /app/target/ELearningBE-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
