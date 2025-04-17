FROM openjdk:21
COPY build/libs /tmp
WORKDIR /tmp
ENTRYPOINT ["java","-jar","--enable-preview", "http-server-javalin-all.jar"]