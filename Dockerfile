# Стадия сборки с кэшированием зависимостей
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Копируем только POM для кэширования зависимостей
COPY pom.xml .

# Скачиваем зависимости (кэшируется если pom.xml не менялся)
RUN mvn dependency:go-offline

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN mvn clean package -Pproduction -DskipTests

# Стадия запуска
FROM eclipse-temurin:21-jre

RUN apt-get update && \
    apt-get install -y --no-install-recommends fontconfig fonts-dejavu && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]