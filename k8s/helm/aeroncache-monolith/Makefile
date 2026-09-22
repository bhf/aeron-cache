install:
	helm install -n aeroncache aeroncache-monolith -f values.yaml --create-namespace .

upgrade:
	helm upgrade -n aeroncache aeroncache-monolith -f values.yaml .

uninstall:
	helm uninstall -n aeroncache aeroncache-monolith --ignore-not-found
