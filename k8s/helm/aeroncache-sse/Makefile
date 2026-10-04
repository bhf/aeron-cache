# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

# Set GHCR=true to pull the image from ghcr.io instead of the local Minikube image (see ../ghcr.mk).
include ../ghcr.mk
GHCR_FLAGS := $(call ghcr_image,aeroncache-sse,image) $(call ghcr_image,aeroncache-media-driver,externalMediaDriver.image) $(ghcr_secret)

install:
	helm install -n aeroncache aeroncache-sse -f values.yaml . $(MEDIA_DRIVER_FLAG) $(GHCR_FLAGS)

install-ephemeral:
	helm install -n aeroncache aeroncache-sse -f values.yaml --set cacheMode=SINGLE --set clusterAddresses=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local --set ephemeralRequestEndpoint=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local:8075 --set ephemeralResponseControlEndpoint=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local:8076 --set service.type=ClusterIP . $(MEDIA_DRIVER_FLAG) $(GHCR_FLAGS)

upgrade:
	helm upgrade -n aeroncache aeroncache-sse -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-sse --ignore-not-found