# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

install:
	helm install -n aeroncache aeroncache-monolith -f values.yaml --create-namespace . $(MEDIA_DRIVER_FLAG)

upgrade:
	helm upgrade -n aeroncache aeroncache-monolith -f values.yaml .

uninstall:
	helm uninstall -n aeroncache aeroncache-monolith --ignore-not-found

# Forward the http (7070), ws (7071) and sse (7072) interface ports to localhost. Runs in the
# foreground; press Ctrl+C to stop. The UI reaches the http API in-cluster, but the browser opens
# the ws/sse connections directly, so these are needed for the UI to work end to end.
port-forward:
	kubectl port-forward -n aeroncache svc/aeroncache-monolith 7070:7070 7071:7071 7072:7072
