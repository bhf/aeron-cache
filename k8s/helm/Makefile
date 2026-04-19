install-backend: install-cluster install-http install-ws install-sse install-http-near
uninstall-backend: uninstall-cluster uninstall-http uninstall-ws uninstall-sse uninstall-http-near

install-frontend: install-ui
uninstall-frontend: uninstall-ui

install-all: install-cluster install-http install-ws install-sse install-http-near install-ui
uninstall-all: uninstall-cluster uninstall-http uninstall-ws uninstall-sse uninstall-http-near uninstall-ui

install-all-singlenode: install-singlenode install-http-singlenode install-ws-singlenode install-sse-singlenode install-ui
uninstall-all-singlenode: uninstall-singlenode uninstall-http uninstall-ws uninstall-sse uninstall-ui

install-backend-singlenode: install-singlenode install-http-singlenode install-ws-singlenode install-sse-singlenode
uninstall-backend-singlenode: uninstall-singlenode uninstall-http uninstall-ws uninstall-sse

install-cluster:
	$(MAKE) -C aeroncache-cluster install

uninstall-cluster:
	$(MAKE) -C aeroncache-cluster uninstall

install-singlenode:
	$(MAKE) -C aeroncache-singlenode install

uninstall-singlenode:
	$(MAKE) -C aeroncache-singlenode uninstall

install-http:
	$(MAKE) -C aeroncache-http-javalin install

install-http-singlenode:
	$(MAKE) -C aeroncache-http-javalin install-singlenode

uninstall-http:
	$(MAKE) -C aeroncache-http-javalin uninstall

install-ws:
	$(MAKE) -C aeroncache-ws-javalin install

install-ws-singlenode:
	$(MAKE) -C aeroncache-ws-javalin install-singlenode

uninstall-ws:
	$(MAKE) -C aeroncache-ws-javalin uninstall

install-sse:
	$(MAKE) -C aeroncache-sse-jooby install

install-sse-singlenode:
	$(MAKE) -C aeroncache-sse-jooby install-singlenode

uninstall-sse:
	$(MAKE) -C aeroncache-sse-jooby uninstall

install-http-near:
	$(MAKE) -C aeroncache-http-near-javalin install

uninstall-http-near:
	$(MAKE) -C aeroncache-http-near-javalin uninstall

install-ui:
	$(MAKE) -C aeroncache-ui-nextjs install

uninstall-ui:
	$(MAKE) -C aeroncache-ui-nextjs uninstall

install-http-clustertools:
	$(MAKE) -C aeroncache-http-clustertools install

uninstall-http-clustertools:
	$(MAKE) -C aeroncache-http-clustertools uninstall