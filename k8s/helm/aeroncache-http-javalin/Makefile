install:
	helm install -n aeroncache aeroncache-http-javalin -f values.yaml .

install-singlenode:
	helm install -n aeroncache aeroncache-http-javalin -f values.yaml --set cacheMode=SINGLE --set clusterAddresses=aeroncache-singlenode-0.aeroncache-singlenode.aeroncache.svc.cluster.local .

upgrade:
	helm upgrade -n aeroncache aeroncache-http-javalin -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-http-javalin