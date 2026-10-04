# Set GHCR=true to pull the image from ghcr.io instead of the local Minikube image (see ../ghcr.mk).
include ../ghcr.mk
GHCR_FLAGS := $(call ghcr_image,aeroncache-http-clustertools,image) $(ghcr_secret)

install:
	helm install -n aeroncache aeroncache-http-clustertools -f values.yaml . $(GHCR_FLAGS)

upgrade:
	helm upgrade -n aeroncache aeroncache-http-clustertools -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-http-clustertools --ignore-not-found