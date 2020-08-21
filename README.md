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
1. Add these properties to the `pom.xml`
```
    <properties>
     <java.version>11</java.version>
     <maven.compiler.source>${java.version}</maven.compiler.source>
     <maven.compiler.target>${java.version}</maven.compiler.target>
    </properties>
```
1. Include Jackson for JSON serialization
```
<!-- https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.11.1</version>
</dependency>
```
1. Indicate the main class
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
1. Build the application running the `mvn clean package` command.

1. Run the application with `java -jar target/hello-world-fwless-1.0-SNAPSHOT.jar`.

1. Verify everything is working `curl localhost:8080/api/hello`.



