# Observability stack (Prometheus + Grafana). Installed into the same `aeroncache` namespace as the
# rest of the deployment; --create-namespace lets it be installed standalone, before the apps.
#
# The upstream Prometheus/Grafana images are public, so there is no GHCR toggle here (unlike the app
# charts) - the kubelet pulls them from Docker Hub on first deploy.

install:
	helm install -n aeroncache aeroncache-observability -f values.yaml --create-namespace .

upgrade:
	helm upgrade -n aeroncache aeroncache-observability -f values.yaml .

uninstall:
	helm uninstall -n aeroncache aeroncache-observability --ignore-not-found

# Forward the Grafana and Prometheus UIs to localhost (foreground; Ctrl+C to stop).
port-forward-grafana:
	kubectl port-forward -n aeroncache svc/aeroncache-grafana 3000:3000

port-forward-prometheus:
	kubectl port-forward -n aeroncache svc/aeroncache-prometheus 9090:9090
