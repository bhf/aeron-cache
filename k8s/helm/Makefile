install-backend: install-cluster install-http install-ws install-sse install-gateway install-http-near
uninstall-backend: uninstall-cluster uninstall-http uninstall-ws uninstall-sse uninstall-gateway uninstall-http-near

install-frontend: install-ui
uninstall-frontend: uninstall-ui

install-all: install-cluster install-http install-ws install-sse install-gateway install-http-near install-ui
uninstall-all: uninstall-cluster uninstall-http uninstall-ws uninstall-sse uninstall-gateway uninstall-http-near uninstall-ui

install-all-ephemeral: install-ephemeral install-http-ephemeral install-ws-ephemeral install-sse-ephemeral install-gateway-ephemeral install-ui
uninstall-all-ephemeral: uninstall-ephemeral uninstall-http uninstall-ws uninstall-sse uninstall-gateway uninstall-ui

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

install-http-near:
	$(MAKE) -C aeroncache-http-near install

uninstall-http-near:
	$(MAKE) -C aeroncache-http-near uninstall

install-ui:
	$(MAKE) -C aeroncache-ui-nextjs install

uninstall-ui:
	$(MAKE) -C aeroncache-ui-nextjs uninstall

install-http-clustertools:
	$(MAKE) -C aeroncache-http-clustertools install

uninstall-http-clustertools:
	$(MAKE) -C aeroncache-http-clustertools uninstall