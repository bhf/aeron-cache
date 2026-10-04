# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

# Set GHCR=true to pull the image from ghcr.io instead of the local Minikube image (see ../ghcr.mk).
# The ephemeral (SINGLE mode) cache runs the same aeroncache-cluster image as the clustered chart.
include ../ghcr.mk
GHCR_FLAGS := $(call ghcr_image,aeroncache-cluster,image) $(call ghcr_image,aeroncache-media-driver,externalMediaDriver.image) $(ghcr_secret)

install:
	helm install -n aeroncache aeroncache-ephemeral -f values.yaml --set cacheMode=SINGLE --create-namespace . $(MEDIA_DRIVER_FLAG) $(GHCR_FLAGS)

upgrade:
	helm upgrade -n aeroncache aeroncache-ephemeral -f values.yaml .

uninstall:
	helm uninstall -n aeroncache aeroncache-ephemeral --ignore-not-found