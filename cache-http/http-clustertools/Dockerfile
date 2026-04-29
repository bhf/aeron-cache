FROM eclipse-temurin:25
COPY build/libs /tmp
WORKDIR /tmp
ENTRYPOINT ["sh", "-c", "exec java --enable-preview --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED -jar http-clustertools-*-all.jar"]