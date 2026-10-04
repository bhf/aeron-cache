# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

# Set GHCR=true to pull images from ghcr.io instead of the local Minikube images (see ../ghcr.mk).
# This chart runs two images: the cluster node and the clustertools sidecar.
include ../ghcr.mk
GHCR_FLAGS := $(call ghcr_image,aeroncache-cluster,image) $(call ghcr_image,aeroncache-http-clustertools,clustertools.image) $(call ghcr_image,aeroncache-media-driver,externalMediaDriver.image) $(ghcr_secret)

install:
	helm install -n aeroncache aeroncache-cluster -f values.yaml --create-namespace . $(MEDIA_DRIVER_FLAG) $(GHCR_FLAGS)

upgrade:
	helm upgrade -n aeroncache aeroncache-cluster -f values.yaml .

uninstall:
	helm uninstall -n aeroncache aeroncache-cluster --ignore-not-found