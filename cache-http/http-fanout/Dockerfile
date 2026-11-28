FROM eclipse-temurin:25-jre
COPY build/libs /tmp
COPY build/agent/opentelemetry-javaagent.jar /tmp/opentelemetry-javaagent.jar
COPY build/agent/opentelemetry-javaagent-extension.jar /tmp/opentelemetry-javaagent-extension.jar
WORKDIR /tmp
ENTRYPOINT ["java", "-jar","--enable-preview", "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED", "http-fanout-all.jar"]
