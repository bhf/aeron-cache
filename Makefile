PROFILE := aeroncache

# Aeron version, taken from the Gradle catalog so the C media driver never drifts
# from the io.aeron:aeron-all the Java apps link against.
AERON_VERSION := $(shell sed -nE 's/^aeron = "([^"]+)".*/\1/p' gradle/libs.versions.toml)

# Helper to point docker and Jib to Minikube's internal docker daemon
MINIKUBE_DOCKER_ENV := eval $$(minikube -p $(PROFILE) docker-env)

all: build-backend build-frontend

pull-images:
	$(MINIKUBE_DOCKER_ENV) && docker pull eclipse-temurin:25-jre
	$(MINIKUBE_DOCKER_ENV) && docker pull node:22-alpine

build-backend: clean-build pull-images build-java build-clustertools build-media-driver
	minikube image ls --profile $(PROFILE) | grep aeroncache

build-media-driver:
	# Compiles the Aeron C media driver (aeronmd) from source, pinned to the
	# catalog Aeron version, into Minikube's Docker daemon.
	$(MINIKUBE_DOCKER_ENV) && docker build docker/aeron-media-driver \
		-t aeroncache-media-driver:latest \
		--build-arg AERON_VERSION=$(AERON_VERSION)

clean-build:
	./gradlew clean build

build-java:
	# Builds all Jib modules directly into Minikube's Docker registry
	$(MINIKUBE_DOCKER_ENV) && ./gradlew jibDockerBuild

build-clustertools:
	# http-clustertools is a Jib module (there is no Dockerfile); build it the same way as the
	# other backend images so it gets a proper /app/jib-classpath-file layer.
	$(MINIKUBE_DOCKER_ENV) && ./gradlew :cache-http:http-clustertools:jibDockerBuild

build-frontend: build-ui

build-ui:
	$(MINIKUBE_DOCKER_ENV) && cd cache-ui/nextjs && docker build . -t aeroncache-ui-nextjs:latest

deploy-cluster:
	helm upgrade --install aeroncache-cluster k8s/helm/aeroncache-cluster/ --set image.tag=latest --set image.pullPolicy=Never

testcontainers-build-all:
	# Builds to the local host daemon (for Testcontainers)
	./gradlew jibDockerBuild
