# hello-world-fwless
Simple java API rest without framework.

This branch aims to showcase how to use [Dekorate](https://github.com/dekorateio/dekorate) to deploy this microservice on a Kubernetes cluster.

## Set up
This project uses [kind](https://kind.sigs.k8s.io/) for running a Kubernetes cluster locally. You can install it following the instructions [here](https://kind.sigs.k8s.io/docs/user/quick-start/#installation).
Once installed, we need to set it up in order to use a local docker registry and install the ingress controller to make Ingress resources work. So follow the next steps:

1.  Create a `kind` cluster with a local registry using the following bash script
```
#!/bin/sh
set -o errexit

# create registry container unless it already exists
reg_name='kind-registry'
reg_port='5000'
running="$(docker inspect -f '{{.State.Running}}' "${reg_name}" 2>/dev/null || true)"
if [ "${running}" != 'true' ]; then
  docker run \
    -d --restart=always -p "${reg_port}:5000" --name "${reg_name}" \
    registry:2
fi

# create a cluster with the local registry enabled in containerd
cat <<EOF | kind create cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
containerdConfigPatches:
- |-
  [plugins."io.containerd.grpc.v1.cri".registry.mirrors."localhost:${reg_port}"]
    endpoint = ["http://${reg_name}:${reg_port}"]
EOF

# connect the registry to the cluster network
docker network connect "kind" "${reg_name}"

# tell https://tilt.dev to use the registry
# https://docs.tilt.dev/choosing_clusters.html#discovering-the-registry
for node in $(kind get nodes); do
  kubectl annotate node "${node}" "kind.x-k8s.io/registry=localhost:${reg_port}";
done
```

2. Install the Ingress controller in the cluster.

You can deploy the [NGINX Ingress Controller](https://github.com/kubernetes/ingress-nginx) running the following command:
```
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/static/provider/kind/deploy.yaml
```

The environment is now ready to deploy our application using `Dekorate`.

Let's begin!

# K8s manifests generation and deploy
## Manifests generation
Generating these manifests using `Dekorate` is very easy, you just need add the proper dependency to the `pom.xml`.

```
 <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>kubernetes-annotations</artifactId>
      <version>1.0.1</version>
 </dependency>
```

We need to enable `Dekorate` by adding an annotation. In this case we will use `@KubernetesApplication` which also gives us access to more Kubernetes specific configuration options.
Edit the App class and add the following annotation.

```
@KubernetesApplication(
        name = "hello-world-fwless-k8s",        
        ports = @Port(name = "web", containerPort = 8080), //1
        host = "fw-app.127.0.0.1.nip.io", //2
        expose = true, //3
        imagePullPolicy = ImagePullPolicy.Always //4
)
```
<1> We need to specify a port in order to prevent dekorate it is about a Web Application, this way a service will be generated in the k8s manifest.
<2> The host under which the application is going to be exposed.
<3> Controls whether the application should be exposed via Ingress.
<4> We use `Always` in order to be able to use an updated image.

Generate the manifests launching a project compilation. Navigate to the directory and run `mvn clean package`. The generated manifests can be found under `target/classes/META-INF/dekorate.

## Build and push the docker image
We need to somehow build and push the (docker) image to the registry.

A basic Dockerfile is provided in the project base directory so you can build the image using the following command:
```
docker build -f Dockerfile -t USERNAME/hello-world-fwless:1.0-SNAPSHOT .
```
Then, tag the image as follows to be able to push it to the local registry:
```
docker tag USERNAME/hello-world-fwless:1.0-SNAPSHOT localhost:5000/hello-world-fwless:1.0-SNAPSHOT
```
Finally, you can push the image to the registry
```
docker push localhost:5000/hello-world-fwless:1.0-SNAPSHOT
```

## Deployment

As we have pushed the image to the local registry, we need to modify manually the kubernetes yml file generated to use that image.
So, replace the following line: 
```
image: amunozhe/hello-world-fwless:1.0-SNAPSHOT
```
by
```
image: localhost:5000/hello-world-fwless:1.0-SNAPSHOT
```

Next, launch the deploy on the cluster:

```
kubectl create ns demo
kubectl apply -f target/classes/META-INF/dekorate/kubernetes.yml -n demo
```

Check the application by opening a browser to http://fw-app.127.0.0.1.nip.io/api/hello
