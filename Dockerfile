# Этап сборки
FROM maven:3.9.11-eclipse-temurin-24 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Этап запуска (лёгкий образ JDK/JRE)
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Оптимизация JVM под контейнер (ограничение памяти Railway)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=95"

EXPOSE 8080
CMD sh -c "java $JAVA_OPTS -jar app.jar"