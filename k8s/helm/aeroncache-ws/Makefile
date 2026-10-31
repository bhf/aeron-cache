# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

# Set JAVAAGENT=true|false to activate (or force off) the bundled OpenTelemetry Java agent's
# Prometheus metrics exporter; leave unset to use the chart's values.yaml default (off). Pair it
# with `make install-observability` to scrape and visualise the metrics.
JAVAAGENT_FLAG := $(if $(JAVAAGENT),--set javaagent.enabled=$(JAVAAGENT))

# Set NEAR_CACHE=true|false to force near-cache-backed hydration (and the near cache management
# endpoints) on or off; leave unset to use the chart's values.yaml default. When on, the workload is
# named "<app>-near" so the near caching variant is visible in the pod name.
NEAR_CACHE_FLAG := $(if $(NEAR_CACHE),--set nearCacheEnabled=$(NEAR_CACHE))

# Set GHCR=true to pull the image from ghcr.io instead of the local Minikube image (see ../ghcr.mk).
include ../ghcr.mk
GHCR_FLAGS := $(call ghcr_image,aeroncache-ws,image) $(call ghcr_image,aeroncache-media-driver,externalMediaDriver.image) $(ghcr_secret)

install:
	helm install -n aeroncache aeroncache-ws -f values.yaml . $(MEDIA_DRIVER_FLAG) $(JAVAAGENT_FLAG) $(NEAR_CACHE_FLAG) $(GHCR_FLAGS)

install-ephemeral:
	helm install -n aeroncache aeroncache-ws -f values.yaml --set cacheMode=SINGLE --set clusterAddresses=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local --set ephemeralRequestEndpoint=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local:8075 --set ephemeralResponseControlEndpoint=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local:8076 --set service.type=ClusterIP . $(MEDIA_DRIVER_FLAG) $(JAVAAGENT_FLAG) $(NEAR_CACHE_FLAG) $(GHCR_FLAGS)

upgrade:
	helm upgrade -n aeroncache aeroncache-ws -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-ws --ignore-not-found