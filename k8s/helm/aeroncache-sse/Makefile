install:
	helm install -n aeroncache aeroncache-sse -f values.yaml .

install-ephemeral:
	helm install -n aeroncache aeroncache-sse -f values.yaml --set cacheMode=SINGLE --set clusterAddresses=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local --set requestPubHost=aeroncache-ephemeral-0.aeroncache-ephemeral.aeroncache.svc.cluster.local --set service.type=ClusterIP --set service.clusterIP=None --set service.publishNotReadyAddresses=true .

upgrade:
	helm upgrade -n aeroncache aeroncache-sse -f values.yaml

uninstall:
	helm uninstall -n aeroncache aeroncache-sse