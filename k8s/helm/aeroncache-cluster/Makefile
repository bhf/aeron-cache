install:
	helm install -n aeroncache aeroncache-cluster -f values.yaml --create-namespace .

upgrade:
	helm upgrade -n aeroncache aeroncache-cluster -f values.yaml .

uninstall:
	helm uninstall -n aeroncache aeroncache-cluster