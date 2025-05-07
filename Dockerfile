# Etapa 1: Construcción del proyecto
FROM maven:3.9.4-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copiamos solo los archivos necesarios (mejor cacheo)
COPY pom.xml .
COPY src ./src

# Compila sin tests para generar el JAR
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final liviana para producción
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copia el JAR generado
COPY --from=builder /app/target/*.jar app.jar

# Expone el puerto (Railway asigna uno dinámico con $PORT)
EXPOSE 8086

# Usa el puerto asignado por Railway o 8081 por defecto
ENV PORT=8086

# Comando de ejecución de Spring Boot
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
