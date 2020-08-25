# hello-world-fwless
Simple pure java API rest.

This project aims to showcase how to use Dekorate to deploy an application, build without a framework, on Kubernetes/Openshift.

Please checkout each container platform from a separate branch. 

The following steps describe how to make a pure java API rest based on maven.

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
4. Indicate the main class
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
5. Edit the main `App` class and paste the code above. This code instantiate an `HttpServer` indicating a path and a port where we want the server listening. It builds also a response to send for the received requests.
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
        System.out.println("Listening in port "+serverPort);
        server.start();
    }

}
```
When we will run this class it will start web server at port 8000 and expose an endpoint which is just printing Hello FrameWorkless world!!

6. Build the application running the `mvn clean package` command.

1. Run the application with `java -jar target/hello-world-fwless-1.0-SNAPSHOT.jar`.

1. Verify everything is working `curl localhost:8080/api/hello`.

Deployment

# Build & run locally
# docker build -f Dockerfile -t auri/hello-world-fwless .
# docker run -i --rm -p 8080:8080 auri/hello-world-fwless

# Build & push to Quay registry
# docker build . -t quay.io/amunozhe/hello-world-fwless
# docker login quay.io
# docker push quay.io/amunozhe/hello-world-fwless:latest

# Deploy the hello-world application manually
# kubectl run hello-world --image=quay.io/amunozhe/hello-world-fwless:latest --port=8080



