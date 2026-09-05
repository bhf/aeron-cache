install:
	helm install -n aeroncache aeroncache-gateway -f values.yaml .

install-ephemeral:
	helm install -n aeroncache aeroncache-gateway -f values.yaml --set cacheMode=SINGLE --set clusterAddresses=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local .

upgrade:
	helm upgrade -n aeroncache aeroncache-gateway -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-gateway
