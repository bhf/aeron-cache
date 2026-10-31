# External Aeron C media driver:
#   Pass EXTERNAL_MEDIA_DRIVER=true to any install target to deploy the interface apps
#   (http, ws, sse, gateway) with the aeronmd C driver as a native sidecar instead of an
#   embedded Java driver; EXTERNAL_MEDIA_DRIVER=false forces it off. Unset uses each
#   chart's values.yaml default (off). Command-line variables propagate to the sub-makes.
#   e.g.  make install-backend-ephemeral EXTERNAL_MEDIA_DRIVER=true
#
# Near cache:
#   Pass NEAR_CACHE=true to any interface-app install target (install-http/-ws/-sse/-gateway, or the
#   aggregates that include them) to deploy that interface with near-cache-backed hydration and the
#   near cache management endpoints enabled; NEAR_CACHE=false forces it off. Unset uses each chart's
#   values.yaml default. Like EXTERNAL_MEDIA_DRIVER it is a command-line variable and propagates to
#   the sub-makes. When near caching is on, the interface's workload (and hence its pods) is named
#   "<app>-near" so the near caching variant stands out in `kubectl get pods`; the Service keeps its
#   plain name, so nothing that addresses the interface by service name changes.
#   The install-*-near aggregates below are shorthand for the matching target with NEAR_CACHE=true.
#   e.g.  make install-backend NEAR_CACHE=true
#         make install-backend-near
#
# Observability (Prometheus + Grafana):
#   `make install-observability` deploys the aeroncache-observability chart (Prometheus + Grafana)
#   into the aeroncache namespace. Pass JAVAAGENT=true to any interface-app install target
#   (install-http/-ws/-sse, or the aggregates that include them) to activate the bundled
#   OpenTelemetry Java agent, which exposes a Prometheus endpoint that the observability stack
#   auto-discovers via pod annotations. JAVAAGENT is unset by default, so apps are never instrumented
#   unless you ask for it. Like EXTERNAL_MEDIA_DRIVER it is a command-line variable and propagates to
#   the sub-makes.
#   e.g.  make install-observability
#         make install-backend JAVAAGENT=true
#         make install-all JAVAAGENT=true
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

install-backend: install-cluster install-http install-ws install-sse install-gateway
uninstall-backend: uninstall-cluster uninstall-http uninstall-ws uninstall-sse uninstall-gateway

install-frontend: install-ui
uninstall-frontend: uninstall-ui

install-all: install-cluster install-http install-ws install-sse install-gateway install-ui
uninstall-all: uninstall-cluster uninstall-http uninstall-ws uninstall-sse uninstall-gateway uninstall-ui

# Full near-caching stack: every interface (http, ws, sse, gateway) deployed with near caching on and
# its workload/pods named "<app>-near". Shorthand for the matching target with NEAR_CACHE=true.
install-backend-near:
	$(MAKE) install-backend NEAR_CACHE=true

install-all-near:
	$(MAKE) install-all NEAR_CACHE=true

install-backend-ephemeral-near:
	$(MAKE) install-backend-ephemeral NEAR_CACHE=true

install-all-ephemeral-near:
	$(MAKE) install-all-ephemeral NEAR_CACHE=true

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

install-ui:
	$(MAKE) -C aeroncache-ui-nextjs install

uninstall-ui:
	$(MAKE) -C aeroncache-ui-nextjs uninstall

install-observability:
	$(MAKE) -C aeroncache-observability install

uninstall-observability:
	$(MAKE) -C aeroncache-observability uninstall

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