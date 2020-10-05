# hello-world-fwless
Simple java rest API without framework


This branch aims to showcase how to use [Dekorate](https://github.com/dekorateio/dekorate) to generate the YAML resources and next deploy this microservice on a kubernetes cluster.

# OpenShift resources generation and deploy
## Manifests generation
Generating these manifests (YAML files) using `Dekorate` is very easy, you just need to add the proper dependency to the `pom.xml`.

```
 <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>openshift-annotations</artifactId>
      <version>1.0.1</version>
 </dependency>
```

Next, we will add a Java annotation for `Dekorate` to tune the YAML resources. It's possible [to configure Dekorate using](https://github.com/dekorateio/dekorate#usage) Java annotations, configuration properties (application.properties), both.
In this case we will use, Java annotations, more specificlly  `@OpenshiftApplication` which also gives us access to more OpenShift specific configuration options.
Edit the App class and add the following annotation.

```
@OpenshiftApplication(
        name = "hello-world-fwless-openshift",        
        ports = @Port(name = "web", containerPort = 8080),  
        expose = true, 
        imagePullPolicy = ImagePullPolicy.Always 
)
```
- We need to prevent Dekorate that a Service should be created. A OpenShift Service is a resource providing a single, constant point of entry to our application. It has an IP address and port that never change while the service exists. Dekorate will generate a OpenShift Service in the manifest if a **`@Port`** is defined.
- **`expose = true`** controls whether the application should be exposed via a `Route` resource accessible from the outside the cluster.
- We use **`Always`** in order to be able to use an updated image.

Trigger the manifests generation. Navigate to the directory and run `mvn clean package`. The generated manifests can be found under `target/classes/META-INF/dekorate`.

**NOTE**: To perform the following steps you need to be connected to a running OpenShift cluster via oc login`

## Building and deploying

Now that we have populated YAML OpenShift resources, we are able to deploy these resources.

We will deploy the application under the namespace `demo` using the yaml resources with the following command:
```
oc new-project demo
oc apply -f target/classes/META-INF/dekorate/openshift.yml
```

A build in OpenShift Container Platform is the process of transforming input parameters into a resulting object. Most often, builds are used to transform source code into a runnable container image. 
To trigger the actual build run the following command:
```
oc start-build hello-world-fwless --from-dir=./target --follow
```

At last, we need to expose the service in order to make it accessible from outside the cluster. For that, list the service using the command:
```
oc get services
```

Copy the name of the service and expose it running:

```
oc expose service hello-world-fwless
```

Now, you should be able to get the route and access the application with a browser. To get the url, tape:

```
oc get routes
```


