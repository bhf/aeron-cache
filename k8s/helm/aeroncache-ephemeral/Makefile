install:
	helm install -n aeroncache aeroncache-ephemeral -f values.yaml --set cacheMode=SINGLE --create-namespace .

upgrade:
	helm upgrade -n aeroncache aeroncache-ephemeral -f values.yaml .

uninstall:
	helm uninstall -n aeroncache aeroncache-ephemeral