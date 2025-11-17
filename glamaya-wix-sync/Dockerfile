FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY target/extracted/dependencies/ ./
COPY target/extracted/snapshot-dependencies/ ./
COPY target/extracted/spring-boot-loader/ ./
COPY target/extracted/application/ ./

RUN chown -R appuser:appgroup /app
USER appuser

# Expose port 8080 to the host
EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
