cd cache-ui/nextjs
docker build . -t aeroncache-ui-nextjs
minikube image load aeroncache-ui-nextjs:latest --profile aeroncache