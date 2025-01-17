FROM openjdk:21
COPY build/libs /tmp
WORKDIR /tmp
ENTRYPOINT ["java","-jar","cache-http-server-all.jar"]