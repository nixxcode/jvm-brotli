---
layout: tutorial
title: "Embed Javalin Into Servlet Container"
author: <a href="https://github.com/mvysny" target="_blank">Martin Vyšný</a>
github: https://github.com/tipsy/javalin-tomcat-embed-example
date: 2018-11-15
summarytitle: Embed Javalin Into Servlet Container
summary: Running Javalin Embedded In Tomcat Without Jetty
language: kotlin
---

## What You'll Create
A WAR application which will contain Javalin without Jetty. You can drop
this WAR file into any Servlet 3.0 container.

## Getting Started

The easiest way to get started is to clone the [javalin-tomcat-embed-example](https://github.com/tipsy/javalin-tomcat-embed-example)
example application:

```bash
git clone https://github.com/tipsy/javalin-tomcat-embed-example
cd javalin-tomcat-embed-example
./gradlew clean appRun
```

This will run Gradle Gretty plugin which in turn launches this WAR app in Tomcat.
When the server boots, you can access the REST endpoint simply by typing
this in your terminal, or opening http://localhost:8080/rest :

```bash
curl localhost:8080/rest/
```

## Looking At The Sources

The project is using Gradle to do standard stuff: declare the project as WAR and
uses the [Gradle Gretty Plugin](https://github.com/gretty-gradle-plugin/gretty)
to easily launch the WAR app in Tomcat (using the `appRun` task).

The interesting bit is the `dependencies` stanza which includes Javalin but omits
the Jetty dependency:

```kotlin
dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("io.javalin:javalin:2.4.0") {
        exclude(mapOf("group" to "org.eclipse.jetty"))
        exclude(mapOf("group" to "org.eclipse.jetty.websocket"))
    }
    compile("org.slf4j:slf4j-simple:1.7.25")
}
```

The servlet itself is very simple:

```kotlin
@WebServlet(urlPatterns = ["/rest/*"], name = "MyRestServlet", asyncSupported = false)
class MyRestServlet : HttpServlet() {
    val javalin = EmbeddedJavalin()
            .get("/rest") { ctx -> ctx.result("Hello!") }
            .createServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}
```

> Note: You must remember to use the `EmbeddedJavalin` class, which has been carefully
designed to not to depend on Jetty. Using the original `Javalin` class
will make the WAR app fail to start with `java.lang.ClassNotFoundException: org.eclipse.jetty.server.Server`.

The Servlet container will automatically auto-discover the servlet (since it's annotated with `@WebServlet`);
any requests to the servlet will be directed straight to Javalin which will then take care
of handling the request properly.
