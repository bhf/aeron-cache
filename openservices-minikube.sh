minikube -n aeroncache service aeroncache-http-javalin --url --profile aeroncache
minikube -n aeroncache service aeroncache-http-near-javalin --url --profile aeroncache
minikube -n aeroncache service aeroncache-ws-javalin --url --profile aeroncache
minikube -n aeroncache service aeroncache-sse-jooby --url --profile aeroncache
minikube -n aeroncache service aeroncache-ui-nextjs --url --profile aeroncache