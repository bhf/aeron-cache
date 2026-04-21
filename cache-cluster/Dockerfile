FROM eclipse-temurin:25
COPY build/libs /tmp
WORKDIR /tmp
ENTRYPOINT ["java","-jar", "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED", "cache-cluster-all.jar"]