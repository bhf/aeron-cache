# Set GHCR=true to pull the image from ghcr.io instead of the local Minikube image (see ../ghcr.mk).
# The UI is published to GHCR as aeroncache-ui (the local Minikube image is aeroncache-ui-nextjs).
include ../ghcr.mk
GHCR_FLAGS := $(call ghcr_image,aeroncache-ui,image) $(ghcr_secret)

install:
	helm install -n aeroncache aeroncache-ui-nextjs -f values.yaml . $(GHCR_FLAGS)

# Point the UI at the monolith service instead of the split http/ws/sse services. Only the
# server-side HTTP API host changes; ws/sse are opened by the browser on localhost (via
# port-forward) and the monolith exposes the same ws/sse ports.
install-monolith:
	helm install -n aeroncache aeroncache-ui-nextjs -f values.yaml --set endpoints.api=http://aeroncache-monolith:7070/api/v1 . $(GHCR_FLAGS)

upgrade:
	helm upgrade -n aeroncache aeroncache-ui-nextjs -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-ui-nextjs --ignore-not-found
