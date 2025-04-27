# Deploying on Minikube (wip)

1. Start a minikube cluster:

```bash
minikube start --cpus 4 --memory 8096 --profile aeroncache
```

2. Launch the dashboard associated with your cluster

```bash
minikube dashboard --profile aeroncache
```

3. Set the docker env and build all images

```bash
eval $(minikube docker-env)
docker build . -t aeroncache-http-javalin
docker build . -t aeroncache-cluster
```

4. Upload images

```bash
minikube image load aeroncache-cluster:latest --profile aeroncache
```

5. List images associated with your cluster

```bash
minikube image ls --profile aeroncache
```

6. Install charts using Helm

```bash
cd k8s/helm/
helm install aeroncache-cluster aeroncache-cluster/ 
```

7. Expose the statefulset associated with the services

```bash
kubectl expose statefulset aeroncache-http --type=NodePort --port=7070
```

Other useful commands:

```bash
kubectl exec aeroncache-http -- nslookup api-server
```

Remove images from a cluster:

```bash
minikube image rm docker.io/library/aeroncache-http:latest --profile aeroncache
```

Uninstall a Helm chart:

```bash
helm uninstall aeroncache-http
```