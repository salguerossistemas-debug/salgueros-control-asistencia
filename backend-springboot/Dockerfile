# 1. CompilaciÃ³n con Maven y Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 2. EjecuciÃ³n con imagen ligera de Java 17 JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=8081
ENV TZ=America/Bogota
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx384m", "-Duser.timezone=America/Bogota", "-jar", "app.jar"]
