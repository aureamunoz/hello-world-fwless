# hello-world-fwless
Simple java API rest without framework


How to use Dekorate to deploy the application on Openshift

We will use [dekorate](https://github.com/dekorateio/dekorate) to generate the manifests needed to deploy the application on Openshift.

Generating this manifests is very easy, you just need add the proper dependency to the `pom.xml` and run
`mvn clean package`. `

```
 <dependency>
      <groupId>io.dekorate</groupId>
      <artifactId>openshift-annotations</artifactId>
      <version>1.0.1</version>
 </dependency>
```

The generated manifests can be found under `target/classes/META-INF/dekorate.

NOTE: To perform the following steps you need to be connected to a running OpenShift cluster via oc login`

Run the following command to create the resources defined in the manifest
```
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


