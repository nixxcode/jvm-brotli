---
layout: default
title: For educators
rightmenu: false
permalink: /for-educators
---

<h1 class="no-margin-top">For educators</h1>
Javalin is well suited for programming courses and for demos/prototypes. This page explains why.

## Simple setup and configuration
A lot of universities still use application servers such as Glassfish or Tomcat when teaching Java web development.
Setting up and configuring these servers for each student requires a lot of effort, and that effort
could be spent teaching students about HTTP and programming instead.

Javalin runs on an embedded Jetty server, and you only need to add the dependency
and write a single line of code to create and start a server. A full "Hello World" app looks like this:
```java
import io.javalin.Javalin;

public class HelloWorld {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000); // create and launch server
        app.get("/", ctx -> ctx.result("Hello World")); // add root endpoint
    }
}
```

This app can be packaged and launched with `java -jar hello-world.jar`, no further configuration required.
This lets you focus your classes on core principles rather than specifics for setting up an application server.

## Small and unopinionated
Javalin is just 3500 LoC running on top of Jetty. There is very little magic,
making it’s easy to fully understand the control-flow of your program.

* No annotations
* No global static state
* No reflection
* No configuration files
* Servlet based

Javalin doesn't care how you build your app, so any knowledge obtained while working
with a Javalin project should transfer easily to other (non Javalin) projects.

## API discoverability
Javalin’s API is built with discoverability in mind.
The server configuration object has a fluent/chainable API,
and the context object has everything needed for handling a HTTP-request.

This lets new users discover the API with their IDE:

<img src="/img/pages/for-educators-discoverability.png" alt="Discoverability">

## Good documentation and tutorials
Javalin's documentation is example-based rather than technical, which allows new users to copy snippets and experiment with them.
Javalin also has tutorials for most common tasks that developers have to solve when starting web-programming.

## Active development
A new (backwards compatible) version of Javalin has been released every month since the first version.
Pull requests and issues are reviewed swiftly, normally every week.
