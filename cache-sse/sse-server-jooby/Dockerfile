FROM eclipse-temurin:25
COPY build/libs /tmp
#COPY build/agent/opentelemetry-javaagent.jar /tmp/opentelemetry-javaagent.jar
#COPY build/agent/opentelemetry-javaagent-extension.jar /tmp/opentelemetry-javaagent-extension.jar
WORKDIR /tmp
#ENTRYPOINT ["java", "-javaagent:/tmp/opentelemetry-javaagent.jar", "-Dotel.javaagent.extensions=/opentelemetry-javaagent-extension.jar", "-jar","--enable-preview", "sse-server-jooby-all.jar"]
ENTRYPOINT ["java", "-jar","--enable-preview", "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED", "sse-server-jooby-all.jar"]