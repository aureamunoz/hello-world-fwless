package org.acme;

import com.sun.net.httpserver.HttpServer;
import io.dekorate.docker.annotation.DockerBuild;
import io.dekorate.kubernetes.annotation.Container;
import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.KubernetesApplication;
import io.dekorate.kubernetes.annotation.Port;
import io.dekorate.kubernetes.annotation.ServiceType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Hello world!
 *
 */

@KubernetesApplication(
        name = "hello-world-fwless-k8s",
        ports = @Port(name = "web", containerPort = 8080),
        host = "fw-app.127.0.0.1.nip.io",
        expose = true, // NEEDED FOR INGRESS
        imagePullPolicy = ImagePullPolicy.Always // TO BE ABLE TO USE UPDATED IMAGE ;-)
)
@DockerBuild(image = "docker.io/amunozhe/hello-world-fwless:1.0-SNAPSHOT")
public class App
{
    public static void main(String[] args) throws IOException {
        int serverPort = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        server.createContext("/api/hello", (exchange -> {
            String respText = "Hello From k8s FrameworkLess world!";
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
