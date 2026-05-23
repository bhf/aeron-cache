FROM eclipse-temurin:25-jre
COPY build/libs /tmp
WORKDIR /tmp
ENTRYPOINT ["sh", "-c", "exec java --enable-preview --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED -jar cache-cluster-*-all.jar"]