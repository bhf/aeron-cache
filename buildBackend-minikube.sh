./gradlew clean build
cd cache-cluster
docker build . -t aeroncache-cluster
minikube image load aeroncache-cluster:latest --profile aeroncache
cd ../cache-http/http-server-javalin/
docker build . -t aeroncache-http-javalin
minikube image load aeroncache-http-javalin:latest --profile aeroncache
cd ../cache-ws/ws-server-javalin/
docker build . -t aeroncache-ws-javalin
minikube image load aeroncache-ws-javalin:latest --profile aeroncache
cd ../cache-sse/sse-server-jooby/
docker build . -t aeroncache-sse-jooby
minikube image load aeroncache-sse-jooby:latest --profile aeroncache
cd ../cache-near/http-server-near-javalin/
docker build . -t aeroncache-http-near-javalin
minikube image load aeroncache-http-near-javalin:latest --profile aeroncache
minikube image ls --profile aeroncache
