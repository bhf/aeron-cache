# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

# Set GHCR=true to pull the image from ghcr.io instead of the local Minikube image (see ../ghcr.mk).
include ../ghcr.mk
GHCR_FLAGS := $(call ghcr_image,aeroncache-gateway,image) $(call ghcr_image,aeroncache-media-driver,externalMediaDriver.image) $(ghcr_secret)

install:
	helm install -n aeroncache aeroncache-gateway -f values.yaml . $(MEDIA_DRIVER_FLAG) $(GHCR_FLAGS)

install-ephemeral:
	helm install -n aeroncache aeroncache-gateway -f values.yaml --set cacheMode=SINGLE --set clusterAddresses=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local . $(MEDIA_DRIVER_FLAG) $(GHCR_FLAGS)

upgrade:
	helm upgrade -n aeroncache aeroncache-gateway -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-gateway --ignore-not-found
