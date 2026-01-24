FROM eclipse-temurin:21
COPY build/libs /tmp
#COPY build/agent/opentelemetry-javaagent.jar /tmp/opentelemetry-javaagent.jar
#COPY build/agent/opentelemetry-javaagent-extension.jar /tmp/opentelemetry-javaagent-extension.jar
WORKDIR /tmp
#ENTRYPOINT ["java", "-javaagent:/tmp/opentelemetry-javaagent.jar", "-Dotel.javaagent.extensions=/opentelemetry-javaagent-extension.jar", "-jar","--enable-preview", "ws-server-javalin-all.jar"]
ENTRYPOINT ["java", "-jar","--enable-preview", "ws-server-javalin-all.jar"]