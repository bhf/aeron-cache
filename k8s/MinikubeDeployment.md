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

## Deploying from GHCR instead of local Minikube images

The `make install-*` targets under `k8s/helm/` build and load images into Minikube by default
(`image.pullPolicy: Never`). To instead pull the released images published to the GitHub Container
Registry by `.github/workflows/publish-ghcr.yml`, pass `GHCR=true` to any install target - no
`make build-backend` / `minikube image load` step is needed:

```bash
cd k8s/helm/
make install-all GHCR=true                 # pull :latest from ghcr.io/bhf/*
make install-all GHCR=true GHCR_TAG=1.2.3   # pull a specific release
```

Useful overrides (see `k8s/helm/ghcr.mk` for the full list):

- `GHCR_OWNER=<owner>` - ghcr.io owner if different from `bhf` (e.g. a fork).
- `GHCR_TAG=<tag>` - image tag; releases are tagged `1.2.3` plus `latest`.
- `GHCR_PULL_POLICY=Always` - re-pull a moving tag such as `latest` on every rollout.
- `IMAGE_PULL_SECRET=<name>` - name of a pre-created pull secret, only needed if the GHCR
  packages are private. Create one with, e.g.:

  ```bash
  kubectl create secret docker-registry ghcr -n aeroncache \
    --docker-server=ghcr.io --docker-username=<user> --docker-password=<token>
  ```

This also covers the external Aeron C media driver (`aeroncache-media-driver`): with `GHCR=true` the
charts that can run it as a sidecar point at the GHCR image too, so `GHCR=true EXTERNAL_MEDIA_DRIVER=true`
works without a local build. The sidecar still defaults to off.