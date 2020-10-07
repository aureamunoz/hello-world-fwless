# hello-world-fwless
Simple java API rest without framework


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
- We need to prevent Dekorate that a Service should be created. An OpenShift Service is a resource providing a single, constant point of entry to our application. It has an IP address and port that never change while the service exists. Dekorate will generate an OpenShift Service in the manifest if a **`@Port`** is defined.
- **`expose = true`** controls whether the application should be exposed via a `Route` resource accessible from the outside the cluster.
- We use **`Always`** in order to be able to use an updated image.

## Building the image
We are going to trigger the image build with jib. In order to use `jib` we need to add the `jib-annotations` dependency. Do edit the `pom.xml` file and add the following dependency:

```
    <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>jib-annotations</artifactId>
    </dependency>
```

Now we need to add two more annotations to the App class. One to disable s2i resources, since we are not using S2I approach for building the container image. 
Generally, docker builds are not allowed in OpenShift, so if we want to use jib, we need to perform the image build locally and then push the image to the registry from where OpenShift will pick it up. 
So that is what the `@JibBuild` annotation does, it tells Dekorate which is the image registry we are going to use, in this case we will use `docker.io`.

**NOTE**: you need to have an account on the image registry to be able to push the image.

Edit the App.java and add these annotations:

```
@S2iBuild(enabled=false)
@JibBuild(registry = "docker.io")
```

At this point, we are set! We can now trigger the manifests generation. Navigate to the directory and run `mvn clean package`. To trigger the image build and push it to the registry accessible from Openshift we can pass `-Ddekorate.build=true -Ddekorate.push=true`. The generated manifests can be found under `target/classes/META-INF/dekorate`.

**NOTE**: we don't need to write a dockerfile. We don't even have to have docker installed to create and publish the docker images. Jib does it for us.


## Deploying

**NOTE**: To perform the following steps you need to be connected to a running OpenShift cluster via oc login`

Now that we have populated YAML OpenShift resources, we are able to deploy these resources.

We will deploy the application under the namespace `demo` using the yaml resources with the following command:

```
oc new-project demo
oc apply -f target/classes/META-INF/dekorate/openshift.yml
```

Finally, get the url to access the application using the following command:

```
oc get route
```

Open a browser to the url and the path of the application:  $URL/api/hello
You should have the following message:

`Hello from OpenShift FrameworkLess world!`


