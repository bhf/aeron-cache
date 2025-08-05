# sse-server-jooby

An SSE interface to Aeron Cache for subscribing to cache updates. Uses [Jooby](https://jooby.io/).

### How to Run

```bash
kubectl apply -f k8s/serviceaccount.yml
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/deployment-statefulset.yml
kubectl apply -f k8s/service-lb.yml
```