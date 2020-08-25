# hello-world-fwless
Simple java API rest without framework


This branch aims to showcase how to use Dekorate to deploy this microservice on Openshift.

We will use [dekorate](https://github.com/dekorateio/dekorate) to generate the manifests needed to deploy the application on Openshift.

Generating this manifests is very easy, you just need add the proper dependency to the `pom.xml`.

```
 <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>kubernetes-annotations</artifactId>
      <version>1.0.1</version>
 </dependency>
```

We also need to add an annotation to enable dekorate. In this case we will use `@KubernetesApplication` which also gives us access to more Kubernetes specific configuration options.
Edit the App class and add the indicated annotation.

```
@KubernetesApplication(name = "hello-world-fwless-k8s", ports = @Port(name = "web", containerPort = 8080))
```
We need to specify a port in order to prevent dekorate it is about a Web Application, this way a service will be generated in the k8s manifest.

NOTE: if you are using minikube a trick that often works is to use a service of type NodePort.

Generate the manifests launching a project compilation. Navigate to the directory and run `mvn clean package`. The generated manifests can be found under `target/classes/META-INF/dekorate.

NOTE: To perform the following steps you need to be connected to a running kubernetes cluster.

Run the following command to create the resources defined in the manifest.
```
kubectl apply -f target/classes/META-INF/dekorate/kubernetes.yml
```

At last, we need to expose the service in order to make it accessible from outside the cluster. For that, list the service using the command:
```
oc get services
```

Copy the name of the service and expose it running:

```
oc expose service hello-world-fwless
```

Now, you should be able to get the address of the service and access the application with a browser. To get the adress, tape:

```
kubectl get services
```

Open a browser to x.x.x.x:y.y/api/hello

Deployment



