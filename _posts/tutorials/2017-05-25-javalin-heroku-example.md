---
layout: tutorial
title: "Deploying Javalin on Heroku"
author: <a href="https://www.linkedin.com/in/davidaase" target="_blank">David Åse</a>
date: 2017-05-25
permalink: /tutorials/heroku
github: https://github.com/tipsy/javalin-heroku-example
summarytitle: Deploying to Heroku
summary: Deploy a Javalin Hello World application on Heroku!
language: java
---

## What is Heroku?
<blockquote>
    <p>
        Heroku is a cloud application platform – a new way of building and deploying web apps.
        Our service lets app developers spend their time on their application code,
        not managing servers, deployment, ongoing operations, or scaling.
        &mdash; <a href="https://www.heroku.com/about">heroku.com</a>
    </p>
</blockquote>

Heroku takes care of everything related to deployment, and gives
you easy access to key commands via their tool Heroku Toolbelt.
It's very easy to get started with (as you'll soon learn), and it
provides a nice free-tier that you can use to deploy your webapps.

## Initial Setup
Before we get started, there are a few things we need to do:

* Create a free Heroku account [(sign up)](https://signup.heroku.com/dc)
* Install [Heroku Toolbelt](https://toolbelt.heroku.com/)
* Install [Maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
* Set up the Javalin Hello World example with Maven [(→ Tutorial)](/tutorials/maven-setup)

## Configuring Maven
This is actually where most of the work is done. In order to easily
deploy a Java application anywhere, you have to create a jar file
containing your application and all of its dependencies.
Open the pom.xml of your Javalin Maven project and add the
following configuration (below your dependencies tag):

~~~markup
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
            <artifactId>maven-assembly-plugin</artifactId>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>single</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <descriptorRefs>
                    <!-- This tells Maven to include all dependencies -->
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
                <archive>
                    <manifest>
                        <mainClass>Main</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
    </plugins>
</build>
~~~

## Configuring Heroku
Before we can configure anything, we actually have to create a
Heroku application. This can be done by using the `heroku create` command.\\
Open a terminal and navigate to your project root, then enter:

~~~bash
heroku create javalin-heroku-example #choose your own application name 
~~~

Now that you have a Heroku application, we have to configure how to
deploy it using Maven. This is pretty straightforward using the Heroku Maven plugin.

We specify the JDK version and the app-name, along with the launch config:
~~~markup
<plugin>
    <groupId>com.heroku.sdk</groupId>
    <artifactId>heroku-maven-plugin</artifactId>
    <version>1.1.3</version>
    <configuration>
        <jdkVersion>1.8</jdkVersion>
        <appName>javalin-heroku-example</appName>
        <processTypes>
            <!-- Tell Heroku how to launch your application -->
            <web>java -jar ./target/javalin-heroku-example-1.0-jar-with-dependencies.jar</web>
        </processTypes>
    </configuration>
</plugin>
~~~
When you've added the Heroku config to your pom,
it should look like [this](https://github.com/tipsy/javalin-heroku-example/blob/master/pom.xml)

## Making Javalin Listen on the Correct Port
The only thing left is making sure Javalin can handle your requests.
Heroku assigns your application a new port every time you deploy it,
so we have to get this port and tell Javalin to use it:

~~~java
import io.javalin.Javalin;

public class Main {

  public static void main(String[] args) {
    Javalin app = Javalin.create()
        .start(getHerokuAssignedPort())
        .get("/", ctx -> ctx.result("Hello Heroku"));
  }

  private static int getHerokuAssignedPort() {
    String herokuPort = System.getenv("PORT");
    if (herokuPort != null) {
      return Integer.parseInt(herokuPort);
    }
    return 7000;
  }

}
~~~

Now we can deploy our application using `mvn heroku:deploy`.

Again, make sure you're in your project root, then enter:
~~~bash
mvn heroku:deploy
~~~

That's it. Our application is now available at `https://<appname>.herokuapp.com/`
