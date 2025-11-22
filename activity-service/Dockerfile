# Runtime stage: solo Java para correr la app
FROM eclipse-temurin:17-jdk

# Carpeta de trabajo dentro del contenedor
WORKDIR /app

# Copiar el JAR generado por Maven desde tu máquina local
COPY target/activity-service-0.0.1-SNAPSHOT.jar app.jar

# Puerto donde escuchará el microservicio
EXPOSE 8082

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]