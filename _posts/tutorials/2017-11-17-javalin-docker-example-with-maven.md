---
layout: tutorial
title: "Deploying Javalin on Docker Container with Maven"
author: <a href="https://www.linkedin.com/in/prasad-marne-7bb85b100/" target="_blank">Prasad Marne</a>
date: 2017-11-17
permalink: /tutorials/docker
github: https://github.com/prasad-marne/javalin-docker
summarytitle: Deploying to Docker Container with Maven
summary: Deploy a Javalin Hello World application on Docker Container!
language: java
---

## What is Docker?
<blockquote>
    <p>
        Docker is a tool designed to make it easier to create, deploy, and run applications by using containers.
        Containers allow a developer to package up an application with all of the parts it needs,
        such as libraries and other dependencies, and ship it all out as one package.
        By doing so, thanks to the container, the developer can rest assured that the application will run on any other
        Linux machine regardless of any customized settings that machine might have that could differ from the machine
        used for writing and testing the code.
        In a way, Docker is a bit like a virtual machine. But unlike a virtual machine, rather than creating a whole 
        virtual operating system, Docker allows applications to use the same Linux kernel as the system that they're 
        running on and only requires applications be shipped with things not already running on the host computer.
        This gives a significant performance boost and reduces the size of the application.
        &mdash; <a href="https://opensource.com/resources/what-docker">opensource.com</a>
    </p>
</blockquote>

## Initial Setup
Before we get started, there are a few things we need to do:

* Set up Docker [(Install Docker)](https://docs.docker.com/engine/installation/)
* Deploy Registry server [(Deploy Registry)](https://docs.docker.com/registry/deploying/)
* Install [Maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
* Set up the Javalin Hello World example with Maven [(→ Tutorial)](/tutorials/maven-setup)

## Configuring Maven
This is actually where most of the work is done. In order to easily
deploy a Java application anywhere, you have to create a jar file
containing your application and all of its dependencies.
Open the pom.xml of your Javalin Maven project and add the
following configuration (below your dependencies tag):

~~~markup
<distributionManagement>
        <repository>
            <id>repo</id>
            <name>Internal release</name>
            <url>your-snapshot-repo-url</url>
        </repository>
</distributionManagement>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>2.3.2</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.1.0</version>
            <executions>
                <!-- Run shade goal on package phase -->
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <shadedArtifactAttached>true</shadedArtifactAttached>
                        <transformers>
                            <!-- add Main-Class to manifest file -->
                            <transformer
                                    implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>HelloWorld</mainClass>
                            </transformer>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
~~~

## Configuring Docker
Before we can configure anything we must create a Dockerfile.
We can create a text file using any editor and name it Dockerfile. 
Copy below contents to the Dockerfile and move this file to root of your project.
~~~markup
FROM openjdk:8-jre-alpine

EXPOSE 7000
ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/javalin/my-javalin.jar"]

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/javalin/my-javalin.jar
~~~
When you've added the Dockerfile to your project,
it should look like [this](https://github.com/prasad-marne/javalin-docker/blob/master/Dockerfile)

To build a docker image for your application we will use dockerfile-maven-plugin
 [(dockerfile-maven-plugin)](https://github.com/spotify/dockerfile-maven).
 Set DOCKER_HOST environment variable as mentioned [(here)](https://github.com/spotify/docker-maven-plugin#setup).
 In the repository section "localhost:5000" is the registry url.
~~~markup
<plugin>
    <groupId>com.spotify</groupId>
    <artifactId>dockerfile-maven-plugin</artifactId>
    <version>1.3.6</version>
    <executions>
        <execution>
            <id>default</id>
            <goals>
                <goal>build</goal>
                <goal>push</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <repository>localhost:5000/javalin</repository>
        <tag>${project.version}</tag>
        <buildArgs>
            <JAR_FILE>${project.build.finalName}-shaded.jar</JAR_FILE>
        </buildArgs>
    </configuration>
</plugin>
~~~
When you've added the Docker config to your pom,
it should look like [this](https://github.com/prasad-marne/javalin-docker/blob/master/pom.xml)

## Making Javalin Listen on the Correct Port
The only thing left is making sure Javalin can handle your requests.
We have exposed port 7000 in Dockerfile. That means that 7000 port on the container is accessible to the outside world.
So we will configure Javalin to listen on "7000"
~~~java
import io.javalin.Javalin;

public class HelloWorld {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        app.get("/", ctx -> ctx.result("Hello World"));
    }

}
~~~
## Build and Push Docker image
Now we can deploy our application using `mvn deploy`.
This will build the docker image and push it to your registry server.
Image name is same as repository value in the pom. 
Additionally we add a tag to image to specify images for different versions.
So image name for this example is localhost:5000/javalin:1.0.0-SNAPSHOT.
Again, make sure you're in your project root, then enter:
~~~bash
mvn deploy
~~~

## Run Docker image 
Now we can run our application using `docker run`.
open terminal then enter:
~~~bash
docker run -d -p 7000:7000 localhost:5000/javalin:1.0.0-SNAPSHOT
~~~

That's it. Our application is now available at http://localhost:7000/
