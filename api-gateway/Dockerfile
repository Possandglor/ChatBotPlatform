FROM docker-nexus.privatbank.ua/library/eclipse-temurin:21.0.5_11-jdk AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Сборка приложения
RUN ./mvnw clean package -DskipTests

FROM docker-nexus.privatbank.ua/library/eclipse-temurin:21.0.5_11-jdk
USER root

COPY --from=builder /app/target/*-runner.jar /deployments/app-runner.jar
WORKDIR /deployments

EXPOSE 8090
ENV PORT=8090

CMD ["java", "-jar", "app-runner.jar"]
