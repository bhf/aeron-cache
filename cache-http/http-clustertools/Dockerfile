FROM eclipse-temurin:25
COPY build/libs /tmp
WORKDIR /tmp
ENTRYPOINT ["java", "-jar","--enable-preview", "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED", "http-clustertools-all.jar"]