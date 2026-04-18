PROFILE := aeroncache

all: build-backend build-frontend

build-backend: clean-build build-cluster build-http build-ws build-sse build-near build-clustertools
	minikube image ls --profile $(PROFILE)

clean-build:
	./gradlew clean build

build-cluster:
	cd cache-cluster && docker build . -t aeroncache-cluster
	minikube image load aeroncache-cluster:latest --profile $(PROFILE)

build-http:
	cd cache-http/http-server-javalin && docker build . -t aeroncache-http-javalin
	minikube image load aeroncache-http-javalin:latest --profile $(PROFILE)

build-ws:
	cd cache-ws/ws-server-javalin && docker build . -t aeroncache-ws-javalin
	minikube image load aeroncache-ws-javalin:latest --profile $(PROFILE)

build-sse:
	cd cache-sse/sse-server-jooby && docker build . -t aeroncache-sse-jooby
	minikube image load aeroncache-sse-jooby:latest --profile $(PROFILE)

build-near:
	cd cache-near/http-server-near-javalin && docker build . -t aeroncache-http-near-javalin
	minikube image load aeroncache-http-near-javalin:latest --profile $(PROFILE)

build-clustertools:
	cd cache-http/http-clustertools && docker build . -t aeroncache-http-clustertools
	minikube image load aeroncache-http-clustertools:latest --profile $(PROFILE)


build-frontend: build-ui

build-ui:
	cd cache-ui/nextjs && docker build . -t aeroncache-ui-nextjs
	minikube image load aeroncache-ui-nextjs:latest --profile $(PROFILE)


testcontainers-build-all: docker-build-cluster docker-build-http docker-build-ws docker-build-sse docker-build-near

docker-build-cluster:
	cd cache-cluster && docker build . -t aeroncache-cluster

docker-build-http:
	cd cache-http/http-server-javalin && docker build . -t aeroncache-http-javalin

docker-build-ws:
	cd cache-ws/ws-server-javalin && docker build . -t aeroncache-ws-javalin

docker-build-sse:
	cd cache-sse/sse-server-jooby && docker build . -t aeroncache-sse-jooby

docker-build-near:
	cd cache-near/http-server-near-javalin && docker build . -t aeroncache-http-near-javalin