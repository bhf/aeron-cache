# Cache-Cluster

## K8s

The service is deployed as a stateful set with 3 replicas.

### How to Run

```bash
kubectl apply -f k8s/serviceaccount.yml
kubectl apply -f k8s/deployment-statefulset.yml
kubectl apply -f k8s/service.yml
```