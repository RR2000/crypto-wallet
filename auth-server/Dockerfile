# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && mkdir /data && chown app:app /data
USER app
WORKDIR /app
COPY --from=build /src/target/auth-server-*.jar app.jar
# database H2 su file nel volume /data, schema gestito da Flyway
ENV WALLET_DB_URL=jdbc:h2:file:/data/wallet
VOLUME /data
EXPOSE 9000
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD wget -qO- http://127.0.0.1:9000/auth/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
