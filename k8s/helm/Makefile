# External Aeron C media driver:
#   Pass EXTERNAL_MEDIA_DRIVER=true to any install target to deploy the interface apps
#   (http, ws, sse, gateway) with the aeronmd C driver as a native sidecar instead of an
#   embedded Java driver; EXTERNAL_MEDIA_DRIVER=false forces it off. Unset uses each
#   chart's values.yaml default (off). Command-line variables propagate to the sub-makes.
#   e.g.  make install-backend-ephemeral EXTERNAL_MEDIA_DRIVER=true
#
# GHCR image source:
#   By default the install targets use the images built into Minikube (image.pullPolicy Never).
#   Pass GHCR=true to any install target to instead pull the released images from ghcr.io
#   (published by .github/workflows/publish-ghcr.yml) - no `make build-*` / minikube load needed.
#   GHCR_OWNER (default bhf), GHCR_TAG (default latest) and IMAGE_PULL_SECRET (for private
#   packages) tune it; see ghcr.mk for the full list. Like EXTERNAL_MEDIA_DRIVER these are
#   command-line variables and propagate to the sub-makes.
#   e.g.  make install-all GHCR=true
#         make install-all GHCR=true GHCR_TAG=1.2.3

install-backend: install-cluster install-http install-ws install-sse install-gateway install-http-near
uninstall-backend: uninstall-cluster uninstall-http uninstall-ws uninstall-sse uninstall-gateway uninstall-http-near

install-frontend: install-ui
uninstall-frontend: uninstall-ui

install-all: install-cluster install-http install-ws install-sse install-gateway install-http-near install-ui
uninstall-all: uninstall-cluster uninstall-http uninstall-ws uninstall-sse uninstall-gateway uninstall-http-near uninstall-ui

install-all-ephemeral: install-ephemeral install-http-ephemeral install-ws-ephemeral install-sse-ephemeral install-gateway-ephemeral install-ui
uninstall-all-ephemeral: uninstall-ephemeral uninstall-http uninstall-ws uninstall-sse uninstall-ui uninstall-gateway

install-backend-ephemeral: install-ephemeral install-http-ephemeral install-ws-ephemeral install-sse-ephemeral install-gateway-ephemeral
uninstall-backend-ephemeral: uninstall-ephemeral uninstall-http uninstall-ws uninstall-sse uninstall-gateway

install-cluster:
	$(MAKE) -C aeroncache-cluster install

uninstall-cluster:
	$(MAKE) -C aeroncache-cluster uninstall

install-ephemeral:
	$(MAKE) -C aeroncache-ephemeral install

uninstall-ephemeral:
	$(MAKE) -C aeroncache-ephemeral uninstall

install-http:
	$(MAKE) -C aeroncache-http install

install-http-ephemeral:
	$(MAKE) -C aeroncache-http install-ephemeral

uninstall-http:
	$(MAKE) -C aeroncache-http uninstall

install-ws:
	$(MAKE) -C aeroncache-ws install

install-ws-ephemeral:
	$(MAKE) -C aeroncache-ws install-ephemeral

uninstall-ws:
	$(MAKE) -C aeroncache-ws uninstall

install-sse:
	$(MAKE) -C aeroncache-sse install

install-sse-ephemeral:
	$(MAKE) -C aeroncache-sse install-ephemeral

uninstall-sse:
	$(MAKE) -C aeroncache-sse uninstall

install-gateway:
	$(MAKE) -C aeroncache-gateway install

install-gateway-ephemeral:
	$(MAKE) -C aeroncache-gateway install-ephemeral

uninstall-gateway:
	$(MAKE) -C aeroncache-gateway uninstall

install-http-near:
	$(MAKE) -C aeroncache-http-near install

uninstall-http-near:
	$(MAKE) -C aeroncache-http-near uninstall

install-ui:
	$(MAKE) -C aeroncache-ui-nextjs install

uninstall-ui:
	$(MAKE) -C aeroncache-ui-nextjs uninstall

install-ui-monolith:
	$(MAKE) -C aeroncache-ui-nextjs install-monolith

uninstall-ui-monolith:
	$(MAKE) -C aeroncache-ui-nextjs uninstall

install-http-clustertools:
	$(MAKE) -C aeroncache-http-clustertools install

uninstall-http-clustertools:
	$(MAKE) -C aeroncache-http-clustertools uninstall

install-monolith:
	$(MAKE) -C aeroncache-monolith install

uninstall-monolith:
	$(MAKE) -C aeroncache-monolith uninstall

# Deploy the monolith backend together with the UI pointed at the monolith service.
install-monolith-all: install-monolith install-ui-monolith
uninstall-monolith-all: uninstall-monolith uninstall-ui-monolith

# Forward the monolith's http/ws/sse ports to localhost (foreground; Ctrl+C to stop).
port-forward-monolith:
	$(MAKE) -C aeroncache-monolith port-forward