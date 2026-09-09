FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY apps/api/.mvn apps/api/.mvn
COPY apps/api/mvnw apps/api/mvnw
COPY apps/api/pom.xml apps/api/pom.xml

RUN chmod +x apps/api/mvnw
RUN cd apps/api && ./mvnw -q -DskipTests dependency:go-offline

COPY apps/api/src apps/api/src

RUN cd apps/api && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/apps/api/target/api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
