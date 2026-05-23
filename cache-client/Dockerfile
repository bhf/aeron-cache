FROM eclipse-temurin:25-jre
COPY build/libs /tmp
WORKDIR /tmp
ENTRYPOINT ["java","-jar","cache-client-all.jar"]