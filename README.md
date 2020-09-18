# hello-world-fwless
Simple java rest API without framework.

This branch aims to showcase how to use [Dekorate](https://github.com/dekorateio/dekorate) to generate the YAML resources and next deploy this microservice on a kubernetes cluster.

## Set up
This project uses [kind](https://kind.sigs.k8s.io/) for running a Kubernetes cluster locally. You can install it following the instructions [here](https://kind.sigs.k8s.io/docs/user/quick-start/#installation).
Once installed, we need to set it up in order to :

- Use a local docker registry needed to provide the image of the microservice when the pod is created.
- Install an Ingress controller managing the access from the host to the cluster of the exposed services.


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

The environment is now ready. We can generate kubernetes manifests using `Dekorate` and proceed to deploy them.

Let's begin!

# K8s manifests generation and deploy
## Manifests generation
Generating these manifests using `Dekorate` is very easy, you just need to add the proper dependency to the `pom.xml`.

```
 <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>kubernetes-annotations</artifactId>
      <version>1.0.1</version>
 </dependency>
```

Next, we will add a Java annotation to tell to `Dekorate` what should be populate during the creation of the MANIFEST for Kubernetes. In this case we will use [`@KubernetesApplication`](https://github.com/dekorateio/dekorate#kubernetes) which also gives us access to more Kubernetes specific configuration options.
Edit the App class and add the following annotation.

```
@KubernetesApplication(
        name = "hello-world-fwless-k8s",        
        ports = @Port(name = "web", containerPort = 8080),  
        expose = true, 
        host = "fw-app.127.0.0.1.nip.io", 
        imagePullPolicy = ImagePullPolicy.Always 
)
```
- We need to prevent Dekorate that a Kubernetes Service should be created. A Kubernetes Service is a resource providing a single, constant point of entry to our application. It has an IP address and port that never change while the service exists. Dekorate will generate a Kubernetes Service in the manifest if a **`@Port`** is defined.
- **`expose = true`** controls whether the application should be exposed via an Ingress resource accessible from the outside the cluster.
- The host under which the application is going to be exposed. It's used by the Ingress resource to deliver the queries addresed to that host to our service.
- We use **`Always`** in order to be able to use an updated image.

Trigger the manifests generation. Navigate to the directory and run `mvn clean package`. The generated manifests can be found under `target/classes/META-INF/dekorate`.

## Building and deploying

Now that we have populated YAML kubernetes resources, we are able to deploy the application on the cluster, we must first create a container image and push it to a local container registry.

### Using docker
A basic Dockerfile is provided in the project base directory so you can build the image using the following command:
```
docker build -f Dockerfile -t USERNAME/hello-world-fwless:1.0-SNAPSHOT .
```
Then, tag the image as follows to be able to push it to the local registry:
```
docker tag USERNAME/hello-world-fwless:1.0-SNAPSHOT localhost:5000/hello-world-fwless:1.0-SNAPSHOT
```
Finally, you can push the image to the local registry
```
docker push localhost:5000/hello-world-fwless:1.0-SNAPSHOT
```

**IMPORTANT**: As the repository of the image has changed, we need to modify manually the kubernetes yml file generated to use that image. Dekorate does not support yet the configuration of the full image name by the user.
So, replace the following line: 
```
image: USERNAME/hello-world-fwless:1.0-SNAPSHOT
```
by
```
image: localhost:5000/hello-world-fwless:1.0-SNAPSHOT
```

Finally, we will deploy the application under the namespace `demo` using the yaml resources with the following command:

```
kubectl create ns demo
kubectl apply -f target/classes/META-INF/dekorate/kubernetes.yml -n demo
```
After a few seconds, check if the application is running and available at the following address opened within your browser `http://fw-app.127.0.0.1.nip.io/api/hello` or do a curl/wget or httpie within a terminal.

**NOTE** Dekorate [allows the user to trigger container image builds and deploy](https://github.com/dekorateio/dekorate#building-and-deploying) after the end of compilation. So, alternatively, you could also delegate the build of the container image and the manifests deployment using the followings hooks provided by Dekorate:
```
mvn clean package -Ddekorate.build=true  -Ddekorate.push=true -Ddekorate.docker.registry="localhost:5000" -Ddekorate.deploy=true
```

### Using jib-maven-plugin

[Jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin) is a Maven plugin for building Docker images. Jib simplifies the containerization since with it, we don't need to write a dockerfile. We don't even have to have docker installed to create and publish the docker images ourselves.
Unsing it via a maven plugin is nice because Jib will catch any changes we make to our application each time we build.

Configure the plugin adding the following code to the `pom.xml` file:

```
    <plugin>
        <groupId>com.google.cloud.tools</groupId>
        <artifactId>jib-maven-plugin</artifactId>
        <version>2.5.2</version>
        <configuration>
          <to>
            <image>localhost:5000/hello-world-fwless:1.0-SNAPSHOT</image>
          </to>
          <allowInsecureRegistries>true</allowInsecureRegistries>
        </configuration>
    </plugin>
```

Build your container image with:

```
mvn compile jib:build
```

Replace the following line in the manifest `target/classes/META-INF/dekorate/kubernetes.yml`: 
```
image: USERNAME/hello-world-fwless:1.0-SNAPSHOT
```
by
```
image: localhost:5000/hello-world-fwless:1.0-SNAPSHOT
```

Finally, we will deploy the application under the namespace `demo` using the yaml resources with the following command:

```
kubectl create ns demo
kubectl apply -f target/classes/META-INF/dekorate/kubernetes.yml -n demo
```
