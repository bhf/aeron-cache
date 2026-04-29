PROFILE := aeroncache

# Helper to point docker and Jib to Minikube's internal docker daemon
MINIKUBE_DOCKER_ENV := eval $$(minikube -p $(PROFILE) docker-env)

all: build-backend build-frontend

build-backend: clean-build build-java build-clustertools
	minikube image ls --profile $(PROFILE) | grep aeroncache

clean-build:
	./gradlew clean build

build-java:
	# Builds all Jib modules directly into Minikube's Docker registry
	$(MINIKUBE_DOCKER_ENV) && ./gradlew jibDockerBuild

build-clustertools:
	$(MINIKUBE_DOCKER_ENV) && cd cache-http/http-clustertools && docker build . -t aeroncache-http-clustertools:latest

build-frontend: build-ui

build-ui:
	$(MINIKUBE_DOCKER_ENV) && cd cache-ui/nextjs && docker build . -t aeroncache-ui-nextjs:latest

testcontainers-build-all:
	# Builds to the local host daemon (for Testcontainers)
	./gradlew jibDockerBuild
