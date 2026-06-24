# ---- ビルドステージ ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY nikki/pom.xml .
COPY nikki/src ./src
RUN mvn clean package -DskipTests

# ---- 実行ステージ ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/nikki-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=${JDBC_DATABASE_URL}"]
