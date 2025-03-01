# http-server-javalin

## K8s

The http server is deployed as a single pod in the same k8s config. 

It could also be a statefulset deployment with 1 replica as it has an embedded AeronCache Client. 

It needs to be exposed as a service.

### How to Run

```bash
kubectl apply -f k8s/serviceaccount.yml
kubectl apply -f k8s/deployment-singlepod.yml
kubectl apply -f k8s/service.yml
```

If you're using MiniKube then be sure to open a tunnel:

```bash
minikube service aeroncache-http --url
```

```bash 
optimus@optimus-lab:~/Workspaces/aeron-cache/cache-http/http-server-javalin$ minikube service aeroncache-http --url --profile sanjeev
😿  service default/aeroncache-http has no node port
❗  Services [default/aeroncache-http] have type "ClusterIP" not meant to be exposed, however for local development minikube allows you to access this !
http://127.0.0.1:35769
❗  Because you are using a Docker driver on linux, the terminal needs to be open to run it.

```
Using the port from above, you can try a GET request e.g. GET http://127.0.0.1:35769/api/v1/cache/321/key


### How to Check it Runs

```bash
kubectl logs aeroncache-http -f
```

Expected output:

```bash
[main] INFO io.javalin.Javalin - Starting Javalin ...
[main] INFO org.eclipse.jetty.server.Server - jetty-11.0.24; built: 2024-08-26T18:11:22.448Z; git: 5dfc59a691b748796f922208956bd1f2794bcd16; jvm 21+35-2513
[main] INFO org.eclipse.jetty.server.session.DefaultSessionIdManager - Session workerName=node0
[main] INFO org.eclipse.jetty.server.handler.ContextHandler - Started o.e.j.s.ServletContextHandler@4c51cf28{/,null,AVAILABLE}
[main] INFO org.eclipse.jetty.server.AbstractConnector - Started ServerConnector@4e928fbf{HTTP/1.1, (http/1.1)}{0.0.0.0:7070}
[main] INFO org.eclipse.jetty.server.Server - Started Server@404bbcbd{STARTING}[11.0.24,sto=0] @708ms
[main] INFO io.javalin.Javalin - 
       __                  ___           _____
      / /___ __   ______ _/ (_)___      / ___/
 __  / / __ `/ | / / __ `/ / / __ \    / __ \
/ /_/ / /_/ /| |/ / /_/ / / / / / /   / /_/ /
\____/\__,_/ |___/\__,_/_/_/_/ /_/    \____/

       https://javalin.io/documentation

[main] INFO io.javalin.Javalin - Javalin started in 157ms \o/
[main] INFO io.javalin.Javalin - Listening on http://localhost:7070/
[main] INFO io.javalin.Javalin - You are running Javalin 6.4.0 (released December 17, 2024).
Building cluster connection...

```

