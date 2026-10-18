# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

# Set JAVAAGENT=true|false to activate (or force off) the bundled OpenTelemetry Java agent's
# Prometheus metrics exporter; leave unset to use the chart's values.yaml default (off). Pair it
# with `make install-observability` to scrape and visualise the metrics.
JAVAAGENT_FLAG := $(if $(JAVAAGENT),--set javaagent.enabled=$(JAVAAGENT))

# Set GHCR=true to pull the image from ghcr.io instead of the local Minikube image (see ../ghcr.mk).
include ../ghcr.mk
GHCR_FLAGS := $(call ghcr_image,aeroncache-http-near,image) $(call ghcr_image,aeroncache-media-driver,externalMediaDriver.image) $(ghcr_secret)

install:
	helm install -n aeroncache aeroncache-http-near -f values.yaml . $(MEDIA_DRIVER_FLAG) $(JAVAAGENT_FLAG) $(GHCR_FLAGS)

upgrade:
	helm upgrade -n aeroncache aeroncache-http-near -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-http-near --ignore-not-found