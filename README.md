# hello-world-fwless
Simple java API rest without framework

This branch aims to showcase :
- How to Use [Dekorate](https://github.com/dekorateio/dekorate) to generate the MANIFESTS,
- To build using JIB the container image,
- To deploy this microservice on an OpenShift cluster.

# OpenShift resources generation and deployment
## Manifests generation

Generating the MANIFESTS (Openshift YAML files) using `Dekorate` is very easy as 
you just need to add the proper dependency to the `pom.xml` .

```xml
 <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>openshift-annotations</artifactId>
      <version>1.0.1</version>
 </dependency>
```

Next, you will add this Java annotation `@OpenshiftApplication` for `Dekorate` to generate the OpenShift YAML resources
but also to tune the configuration.

So, edit the Java `App class` and add the following annotation.

```java
@OpenshiftApplication(
        name = "hello-world-fwless-openshift",        
        ports = @Port(name = "web", containerPort = 8080),  
        expose = true, 
        imagePullPolicy = ImagePullPolicy.Always 
)
```
- To tell to Dekorate that a Kubernetes `Service` should be created, then we configure the **`@Port`** parameter to specify the name of the service to be used and port.
- **`expose = true`** controls whether the application should be accessible outside of the cluster and that an Openshift `Route` resource is create.
- The parameter **`Always`** of the parameter `ImagePullPolicy` allows deploying or redeploying applications with images updated within the container registry.

## Building the image

To build the container image on the laptop of the developer, we will use the [`docker`] tool combined with Dekorate.

So, edit the `pom.xml` file and add the following dependency to :

```xml
    <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>docker-annotations</artifactId>
    </dependency>
```

To customize what Dekorate should do during the build step, we will then perform some modifications as described hereafter.

1. To bypass the generation of the Openshift `Build` and `BuildConfig` resources used by Openshift to perform a container build on the cluster, we will then add a new Java annotation - `@S2iBuild` and change the parameter `enabled=false`,
2. To tell to docker to push the image build locally to the `docker.io` registry, the following annotation must be then added: `@DockerBuild(registry = "docker.io")`

**NOTE**: you need to have an account on the image registry and be logged with `docker login` to be able to push the image.

Edit the Java `App` class and add these annotations:

```java
@S2iBuild(enabled=false)
@DockerBuild(registry = "docker.io")
```

At this point we are set, and we can now trigger the generation of manifests !

Within a terminal, navigate to the directory of this project and execute the following maven command.
To trigger the build of the image and to push it to the registry, we will then pass the following parameters `-Ddekorate.build=true -Ddekorate.push=true` to the command:

```
mvn clean package -Ddekorate.build=true -Ddekorate.push=true
```

**REMARK**: The generated manifests can be found under the following path: `target/classes/META-INF/dekorate`.

**NOTE**: we need a dockerfile to build a container that runs the application. Such file is provided at the project directory. Also, docker must be installed in your machine.

## Deploying the Application on the cluster

**NOTE**: To perform the following steps you need to be connected to a running OpenShift cluster via the command `oc login`

To deploy the application, we will first create a new project or namespace: 
```bash
oc new-project demo
```

Next, the application will be deployed using the following command:
```bash
oc apply -f target/classes/META-INF/dekorate/openshift.yml
```

Finally, in order to access the service exposed, get the url of the route using the following command:

```
oc get route
```

Open a browser to the url and the path of the application: `$URL/api/hello`
You should see the following message:

`Hello from OpenShift FrameworkLess world!`