install:
	helm install -n aeroncache aeroncache-ui-nextjs -f values.yaml .

# Point the UI at the monolith service instead of the split http/ws/sse services. Only the
# server-side HTTP API host changes; ws/sse are opened by the browser on localhost (via
# port-forward) and the monolith exposes the same ws/sse ports.
install-monolith:
	helm install -n aeroncache aeroncache-ui-nextjs -f values.yaml --set endpoints.api=http://aeroncache-monolith:7070/api/v1 .

upgrade:
	helm upgrade -n aeroncache aeroncache-ui-nextjs -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-ui-nextjs --ignore-not-found
