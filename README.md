# hello-world-fwless

Simple java API rest.

This project aims to showcase how to use [Dekorate](dekorate.io) to generate the Kubernetes MANIFEST (YAML resources) 
to build a container image of an application designed without a framework but based on Java classes only and 
deploy it easily next Kubernetes/Openshift.

*NOTE*

The master branch contains just the code of the Java application exposing a REST endpoint.
Depending on the container platform and image building tool that you would like to use, you will find different branches on this repository:

|Container Platform|Image building tool|git branch|
|------------------|:-----------------:|---------:|
| `Kubernetes`     |  `Docker`         |[`dekorate-4-k8s-docker`](https://github.com/aureamunoz/hello-world-fwless/tree/dekorate-4-k8s-docker)    |
| `Kubernetes`     |  `JIB`            |[`dekorate-4-k8s-jib`](https://github.com/aureamunoz/hello-world-fwless/tree/dekorate-4-k8s-jib)    |
| `OpenShift`      |  `Docker`         |[`dekorate-4-ocp-docker`](https://github.com/aureamunoz/hello-world-fwless/tree/dekorate-4-ocp-docker)   |
| `OpenShift`      |  `JIB`            |[`dekorate-4-ocp-jib`](https://github.com/aureamunoz/hello-world-fwless/tree/dekorate-4-ocp-jib)    |

The following steps describe how to create a maven project, configure it and add the needed maven dependency to develop
a pure java API rest.

1. Create a Maven project with an initial `pom.xml` file
```
mvn archetype:generate \
  -DgroupId=org.acme \
      -DartifactId=hello-world-fwless \
      -DarchetypeArtifactId=maven-archetype-quickstart \
      -DinteractiveMode=false

```
2. Add these properties to the `pom.xml`
```
    <properties>
     <java.version>11</java.version>
     <maven.compiler.source>${java.version}</maven.compiler.source>
     <maven.compiler.target>${java.version}</maven.compiler.target>
    </properties>
```
3. Include Jackson for JSON serialization
```
<!-- https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.11.1</version>
</dependency>
```
4. Specify the `main class` used as entry point within the Java JAR archive to launch the application
``` 
    <build>
     <pluginManagement>
     <plugins>
       <plugin>
         <artifactId>maven-jar-plugin</artifactId>
         <version>3.2.0</version>
         <configuration>
           <archive>
             <manifest>
               <mainClass>org.acme.App</mainClass>
             </manifest>
           </archive>
         </configuration>
       </plugin>
     </plugins>
    </pluginManagement>
    </build>
```
5. Edit the main `App` java class and paste the code above. This code instantiates a `HttpServer` and will run it at the specified. 
   The endpoint or URI path to call the service is define using a `HttpContext` object. The response to be populated when a HTTP request
   is receive is managed with an `OutputStream` and `HttpExchange`.
```
package org.acme;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App 
{
    public static void main(String[] args) throws IOException {
        int serverPort = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        server.createContext("/api/hello", (exchange -> {
            String respText = "Hello FrameWorkless world!";
            exchange.sendResponseHeaders(200, respText.getBytes().length);
            OutputStream output = exchange.getResponseBody();
            output.write(respText.getBytes());
            output.flush();
            exchange.close();
        }));
        server.setExecutor(null); // creates a default executor
        System.out.println("Listening on port "+serverPort);
        server.start();
    }

}
```
When you will launch using your IDEA the `App` java class, a web server listening on port 8000 will be started and will expose
an endpoint. If you issue a curl request at the address `localhost:8080/api/hello`, then the following message `Hello FrameWorkless world!`
will be printed.

6. Compile the application using the `mvn clean package` command.

7. Launch the application within a terminal by executing this command: `java -jar target/hello-world-fwless-1.0-SNAPSHOT.jar`.

8. Verify if the endpoint is able to reply to a `curl localhost:8080/api/hello` request.

## Containerization

A Dockerfile is provided in order to build a container that runs the application.

If you have docker installed in your machine, within a terminal you can build the image with:

```
docker build -f Dockerfile -t $USER/hello-world-fwless .
```

Then, run the image using this command:

```
docker run -i --rm -p 8080:8080 $USER/hello-world-fwless
```
Check if the endpoint is responding with `curl localhost:8080/api/hello` or open a browser to `http://localhost:8080/api/hello`.

