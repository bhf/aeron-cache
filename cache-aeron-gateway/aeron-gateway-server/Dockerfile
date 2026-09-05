FROM eclipse-temurin:25-jre
COPY build/libs /tmp
WORKDIR /tmp
ENTRYPOINT ["java", "-jar","--enable-preview", "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED", "aeron-gateway-server-all.jar"]
