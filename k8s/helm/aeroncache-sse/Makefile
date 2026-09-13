# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

install:
	helm install -n aeroncache aeroncache-sse -f values.yaml . $(MEDIA_DRIVER_FLAG)

install-ephemeral:
	helm install -n aeroncache aeroncache-sse -f values.yaml --set cacheMode=SINGLE --set clusterAddresses=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local --set requestPubHost=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local --set service.type=ClusterIP --set service.clusterIP=None --set service.publishNotReadyAddresses=true . $(MEDIA_DRIVER_FLAG)

upgrade:
	helm upgrade -n aeroncache aeroncache-sse -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-sse --ignore-not-found