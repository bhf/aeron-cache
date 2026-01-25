install:
	helm install -n aeroncache aeroncache-singlenode -f values.yaml --set cacheMode=SINGLE --create-namespace .

upgrade:
	helm upgrade -n aeroncache aeroncache-singlenode -f values.yaml .

uninstall:
	helm uninstall -n aeroncache aeroncache-singlenode