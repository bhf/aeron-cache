# Set EXTERNAL_MEDIA_DRIVER=true|false to force the external Aeron C media driver sidecar
# on or off; leave unset to use the chart's values.yaml default (off).
MEDIA_DRIVER_FLAG := $(if $(EXTERNAL_MEDIA_DRIVER),--set externalMediaDriver.enabled=$(EXTERNAL_MEDIA_DRIVER))

install:
	helm install -n aeroncache aeroncache-http-near -f values.yaml . $(MEDIA_DRIVER_FLAG)

upgrade:
	helm upgrade -n aeroncache aeroncache-http-near -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-http-near --ignore-not-found