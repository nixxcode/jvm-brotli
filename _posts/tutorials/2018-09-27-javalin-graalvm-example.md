---
layout: tutorial
title: "Running Javalin on GraalVM (22MB total size)"
author: <a href="https://github.com/birdayz" target="_blank">Johannes Brüderl</a>
github: https://github.com/tipsy/graal-javalin
notice: This tutorial originally appeared on <a href="https://nerden.de/posts/microservice_graalvm/">https://nerden.de</a> and was republished with the authors permission.
date: 2018-09-27
summarytitle: Running on GraalVM (22MB total size)
summary: Building a 22 Megabytes Microservice with Docker, Java, Javalin and GraalVM
language: java
---

Oracle’s GraalVM allows for ahead-of-time (AOT) compilation of JVM applications. This means, instead of running a JVM process to execute your application, the compiler builds a native binary. How does it work? On a very high level, a basic runtime (called SubstrateVM) is compiled into the binary, as well as the actual application. Sounds a little like Go, which also includes a small runtime for e.g. garbage collection. In this article, I’ll show how to build a small sample restful webservice with GraalVM native compilation. The sample application is written in Java. Why would somebody even be interested in native compilation of a JVM application? On my day job at E.ON I sadly still have to work a lot with Java applications. Our tech stack is completely cloud native - we run almost everything on Kubernetes. Our applications are “typical” 12 Factor applications. In such a dockerized environment, I think there are three major reasons why native compilation would be interesting.

*   Application startup time

This may not be entirely Java’s fault. We’re using Spring Boot, and startup is really slow. I usually have to tune my Kubernetes readiness probe to not check earlier than 20 seconds after starting the pod. And that’s for a very small application - 500 lines of code.

*   Memory Footprint

In my experience, you don’t want to give your Spring Boot applications less than 512MB of memory. Otherwise it can take multiple minutes to start. While Java and especially JVM overhead are to blame here, this is just as well a Framework problem. It’s no secret that Spring is very bloated and uses a lot of reflection.

*   Application size

The size of Java application containers is another problem. It is very annoying, but not critical. The smallest JRE I can find is ~65MB large (alpine based openJDK). If you use Spring Boot, you’ll have at least a ~40MB large fat jar for your application. If your application is larger, it will obviously be more. That’s a minimum of 100MB per container. Note that the JRE layer of the Docker image might be reused by multiple Docker image, so it is not really 100MB+ for each image of your app. While I think it’s certainly bearable to have 100MB+ large hello world applications in 2018, it’s just weak if I can have a 6MB Go binary.

GraalVM AOT compilation might improve this situation. I expect startup time to be pretty much instant without the need for a JVM, and application size to be significantly smaller. GraalVM has some serious limitations, because several features of the JVM do not play well with static compilation. A full list can be found [here](https://github.com/oracle/graal/blob/master/substratevm/LIMITATIONS.md). The documentation is crystal clear here: Dynamic Class Loading is and will not be supported. Instead, the compiler analyzes the code and compiles all required classes into the binary. In combination with reflection, this becomes a nightmare for the current Java ecosystem. Many libraries and frameworks use reflection to dynamically instantiate classes. GraalVM does not handle this very well, in many cases additional compiler configuration has to be provided. One of the reasons is, that a call to e.g. Class.forName() may be based on runtime information. A very simple example:

```java
if (someVariable) {
    Class.forName("SomeClazz")
    ...
}
```


Since the value of someVariable is not known at compile time, the compiler can not know whether to include “SomeClazz”. Not to mention that it’s just a string, and the compiler has to search for this class on the classpath at compile time. If the compiler decides to include this class, it will just do that and throw an error if the class is not found. That’s nice. However, this is only best-effort. There is no guarantee that all required classes are included at compile time - which means that classes may be missing, and runtime errors are thrown when they are being instantiated. There are many more limitations, for a full reference head over to GraalVM’s [documentation](https://github.com/oracle/graal/blob/master/substratevm/LIMITATIONS.md). As a proof of concept, I was looking for a rest library without excess usage of reflect. Obviously it’s not spring boot - I chose [javalin.io](https://javalin.io). It’s just a rest library on top of Jetty, that’s it.

Getting started
===============

While I recommend performing builds in Docker, it’s very helpful to install GraalVM locally. I use [sdkman](https://sdkman.io), it eases the management of JDKs. If you don’t have sdkman installed already:

`curl -s "https://get.sdkman.io" | bash`

Install GraalVM JDK:

`sdk install java 1.0.0-rc-16-grl && sdk use java 1.0.0-rc-16-grl`

Let’s start with a very simple Hello World:

```java
public class Main {
    public static void main(String[] args) {
        Test t = new Test();
        t.setSomeValue("Hello World!");
        Javalin app = Javalin.create().start(7000);
        app.get("/", ctx -> ctx.json(t));
    }
}
```

In addition, we must not forget to declare the necessary dependencies. We must include Jackson, as it will be loaded at runtime (d’uh). The same case for a SLF4J binding, Javalin recommends to use slf4j-simple.

```java
compile group: 'io.javalin', name: 'javalin', version: '2.2.0'
compile group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: '2.9.6'
compile group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.25'
compile group: 'org.graalvm', name: 'graal-sdk', version: '1.0.0-rc6'
```

Also, we’ll need to build a fat jar that includes all classes and jars of our application.

```java
task fatJar(type: Jar) {
    manifest {
        attributes 'Implementation-Title': 'Gradle Jar File Example',
                'Implementation-Version': version,
                'Main-Class': 'de.nerden.samples.graal.Main'
    }
    baseName = project.name + '-all'
    from { configurations.compile.collect { it.isDirectory() ? it : zipTree(it) } }
    with jar
}
```

Nothing very special so far. To build the native application executable, GraalVM provides the tool `native-image`. Let’s try it out:

```text
j0e@thinkpad  ~/projects/graal-javalin  master ● ? ⍟1  native-image -jar ./build/libs/graal-javalin-all-1.0-SNAPSHOT.jar
Build on Server(pid: 28578, port: 34643)*
[graal-javalin-all-1.0-SNAPSHOT:28578]    classlist:   2,977.05 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]        (cap):     963.06 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]        setup:   1,663.57 ms
[ForkJoinPool-3-worker-3] INFO org.eclipse.jetty.util.log - Logging initialized @5682ms to org.eclipse.jetty.util.log.Slf4jLog
[graal-javalin-all-1.0-SNAPSHOT:28578]   (typeflow):  10,510.28 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]    (objects):   6,598.95 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]   (features):     110.60 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]     analysis:  17,612.10 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]     universe:     859.27 ms
error: unsupported features in 8 methods
Detailed message:
Error: Unsupported method sun.nio.ch.InheritedChannel.soType0(int) is reachable: Native method. If you intend to use the Java Native Interface (JNI), specify -H:+JNI and see also -H:JNIConfigurationFiles=<path> (use -H:+PrintFlags for details)
To diagnose the issue, you can add the option --report-unsupported-elements-at-runtime. The unsupported element is then reported at run time when it is accessed the first time.
...
...
```

Okay, we need the flag -H:+JNI. That one is quite easy, just add the flag to the command and this problem is solved:

```text
 j0e@thinkpad  ~/projects/graal-javalin  master ● ? ⍟1  native-image -jar ./build/libs/graal-javalin-all-1.0-SNAPSHOT.jar -H:+JNI
Build on Server(pid: 28578, port: 34643)
[graal-javalin-all-1.0-SNAPSHOT:28578]    classlist:     753.67 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]        (cap):     528.63 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]        setup:     776.76 ms
[ForkJoinPool-15-worker-0] INFO org.eclipse.jetty.util.log - Logging initialized @616692ms to org.eclipse.jetty.util.log.Slf4jLog
[graal-javalin-all-1.0-SNAPSHOT:28578]   (typeflow):   5,934.19 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]    (objects):   6,646.13 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]   (features):      83.06 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]     analysis:  13,491.56 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]     universe:     519.25 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]      (parse):   2,360.81 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]     (inline):   3,674.24 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]    (compile):  15,925.13 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]      compile:  22,729.43 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]        image:   1,426.49 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]        write:     280.71 ms
[graal-javalin-all-1.0-SNAPSHOT:28578]      [total]:  40,064.13 ms
```

So compilation was apparently successful. The ugliness starts when we run it:

```text
 j0e@thinkpad  ~/projects/graal-javalin  master ● ? ⍟1  ./graal-javalin-all-1.0-SNAPSHOT                          ✔  33695  00:53:54
[main] INFO io.javalin.Javalin -
 _________________________________________
|        _                  _ _           |
|       | | __ ___   ____ _| (_)_ __      |
|    _  | |/ _` \ \ / / _` | | | '_ \     |
|   | |_| | (_| |\ V / (_| | | | | | |    |
|    \___/ \__,_| \_/ \__,_|_|_|_| |_|    |
|_________________________________________|
|                                         |
|    https://javalin.io/documentation     |
|_________________________________________|
-------------------------------------------------------------------
Missing dependency 'Slf4j simple'. Add the dependency.

pom.xml:
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.25</version>
</dependency>

build.gradle:
compile "org.slf4j:slf4j-simple:1.7.25"
-------------------------------------------------------------------
Visit https://javalin.io/documentation#logging if you need more help
[main] INFO io.javalin.Javalin - Starting Javalin ...
[main] ERROR io.javalin.Javalin - Failed to start Javalin
java.lang.IllegalArgumentException: Class org.eclipse.jetty.servlet.ServletMapping[] is instantiated reflectively but was never registered. Register the class by using org.graalvm.nativeimage.RuntimeReflection
        at java.lang.Throwable.<init>(Throwable.java:265)
        at java.lang.Exception.<init>(Exception.java:66)
        at java.lang.RuntimeException.<init>(RuntimeException.java:62)
        at java.lang.IllegalArgumentException.<init>(IllegalArgumentException.java:52)
        at com.oracle.svm.core.genscavenge.graal.AllocationSnippets.checkDynamicHub(AllocationSnippets.java:162)
        at org.eclipse.jetty.util.ArrayUtil.addToArray(ArrayUtil.java:91)
        at org.eclipse.jetty.servlet.ServletHandler.addServletWithMapping(ServletHandler.java:907)
        at org.eclipse.jetty.servlet.ServletContextHandler.addServlet(ServletContextHandler.java:462)
        at io.javalin.core.util.JettyServerUtil.initialize(JettyServerUtil.kt:71)
        at io.javalin.Javalin.start(Javalin.java:136)
        at io.javalin.Javalin.start(Javalin.java:103)
        at de.nerden.samples.graal.Main.main(Main.java:10)
        at com.oracle.svm.core.JavaMainWrapper.run(JavaMainWrapper.java:163)
```

Reflection didn’t work. This is not a big surprise, but shows the fundamental weakness of GraalVM: it can’t guarantee that your application works, even if it compiles.

To fix this issue, we have to tell GraalVM that the class ServletMapping has to be included in the binary. Since it’s reflection and no ‘normal’ part of the code, it didn’t detect it. There are two possibilities to do this: code-based and JSON configuration-based. I tested both, I think I prefer the json approach but in the end it does not really matter. Add a file with the following contents to your project:

```js
[
  {
    "name": "[Lorg.eclipse.jetty.servlet.ServletMapping;",
    "allDeclaredFields": true,
    "allPublicFields": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "org.slf4j.impl.StaticLoggerBinder",
    "allDeclaredFields": true,
    "allPublicFields": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "com.fasterxml.jackson.databind.ObjectMapper",
    "allDeclaredFields": true,
    "allPublicFields": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "de.nerden.samples.graal.Test",
    "allDeclaredFields": true,
    "allPublicFields": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  }
]
```

Note the special notation `[Lorg.eclipse.jetty.servlet.ServletMapping;`. This is necessary because in this case, an array of ServletMapping objects is being reflectively instantiated. In addition, i’ve added slf4j and Jackson classes, so they are found at runtime. In both cases, runtime errors are thrown because reflection didn’t work. Also, we have to add our own classes to the reflection list. If we don’t do this, the following cryptic exception will be thrown when performing a request:

```text
[qtp1024494636-165] WARN io.javalin.core.ExceptionMapper - Uncaught exception
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class de.nerden.samples.graal.Test and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
        at java.lang.Throwable.<init>(Throwable.java:265)
        at java.lang.Exception.<init>(Exception.java:66)
        at java.io.IOException.<init>(IOException.java:58)
        at com.fasterxml.jackson.core.JsonProcessingException.<init>(JsonProcessingException.java:33)
        at com.fasterxml.jackson.databind.JsonMappingException.<init>(JsonMappingException.java:237)
        at com.fasterxml.jackson.databind.exc.InvalidDefinitionException.<init>(InvalidDefinitionException.java:38)
        at com.fasterxml.jackson.databind.exc.InvalidDefinitionException.from(InvalidDefinitionException.java:77)
        at com.fasterxml.jackson.databind.SerializerProvider.reportBadDefinition(SerializerProvider.java:1191)
        at com.fasterxml.jackson.databind.DatabindContext.reportBadDefinition(DatabindContext.java:312)
        at com.fasterxml.jackson.databind.ser.impl.UnknownSerializer.failForEmpty(UnknownSerializer.java:71)
        at com.fasterxml.jackson.databind.ser.impl.UnknownSerializer.serialize(UnknownSerializer.java:33)
        at com.fasterxml.jackson.databind.ser.DefaultSerializerProvider._serialize(DefaultSerializerProvider.java:480)
        at com.fasterxml.jackson.databind.ser.DefaultSerializerProvider.serializeValue(DefaultSerializerProvider.java:319)
        at com.fasterxml.jackson.databind.ObjectMapper._configAndWriteValue(ObjectMapper.java:3905)
        at com.fasterxml.jackson.databind.ObjectMapper.writeValueAsString(ObjectMapper.java:3219)
        at io.javalin.json.JavalinJackson.toJson(JavalinJackson.kt:26)
        at io.javalin.json.JavalinJson$toJsonMapper$1.map(JavalinJson.kt:28)
        at io.javalin.json.JavalinJson.toJson(JavalinJson.kt:32)
        at io.javalin.Context.json(Context.kt:510)
        at de.nerden.samples.graal.Main.lambda$main$0(Main.java:11)
        at de.nerden.samples.graal.Main$$Lambda$925/1179449634.handle(Unknown Source)
        at io.javalin.security.SecurityUtil.noopAccessManager(SecurityUtil.kt:22)
        at io.javalin.Javalin$$Lambda$928/1713301975.manage(Unknown Source)
        at io.javalin.Javalin.lambda$addHandler$0(Javalin.java:485)
        at io.javalin.Javalin$$Lambda$931/1107122283.handle(Unknown Source)
        at io.javalin.core.JavalinServlet$service$2$1.invoke(JavalinServlet.kt:48)
        at io.javalin.core.JavalinServlet$service$2$1.invoke(JavalinServlet.kt:20)
        at io.javalin.core.JavalinServlet$service$1.invoke(JavalinServlet.kt:145)
        at io.javalin.core.JavalinServlet$service$2.invoke(JavalinServlet.kt:43)
        at io.javalin.core.JavalinServlet.service(JavalinServlet.kt:109)
        at io.javalin.core.util.JettyServerUtil$initialize$httpHandler$1.doHandle(JettyServerUtil.kt:59)
        at org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:203)
        at org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:473)
        at org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:1564)
        at org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:201)
        at org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1242)
        at org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:144)
        at org.eclipse.jetty.server.handler.HandlerList.handle(HandlerList.java:61)
        at org.eclipse.jetty.server.handler.StatisticsHandler.handle(StatisticsHandler.java:174)
        at org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:132)
        at org.eclipse.jetty.server.Server.handle(Server.java:503)
        at org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:364)
        at org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:260)
        at org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:305)
        at org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:103)
        at org.eclipse.jetty.io.ChannelEndPoint$2.run(ChannelEndPoint.java:118)
        at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:765)
        at org.eclipse.jetty.util.thread.QueuedThreadPool$2.run(QueuedThreadPool.java:683)
        at java.lang.Thread.run(Thread.java:748)
        at com.oracle.svm.core.posix.thread.PosixJavaThreads.pthreadStartRoutine(PosixJavaThreads.java:238)
```

The reason is: Jackson uses reflection to marshal/unmarshal json. Once configured properly, it works.

You can try it out on your own! `docker run --net=host birdy/graal-javalin`

Perform the sample call: `curl localhost:7000`

```text
 j0e@thinkpad  ~/projects/graal-javalin  master ? ⍟2  curl localhost:7000                                         ✔  33707  01:15:12
{"abc":"LOL"}%
```

Yay. And the startup time is instant. No JVM!

So, how did this turn out, looking at my 3 points of criticism?

Application startup time
------------------------

The application starts instantly. While Javalin is starting very quickly even on the JVM (~1-2 Seconds), this will be VERY appealing for CLI tools.

Memory Footprint
----------------

Measuring memory usage of a process is not very straight-forward. There are several metrics - according so some post on [https://stackoverflow.com/questions/131303/how-to-measure-actual-memory-usage-of-an-application-or-process](Stackoverflow) RSS is a good metric: So let’s use check this out:

```text
cat /proc/7812/status
VmRSS:     18260 kB
```

18 MB, that looks quite nice. Note that I didn’t perform any load tests, these are just some manual tests with no load. With some more requests with curl it went up to 25MB. The good thing: we have the luxury that we can directly compare this to the same application, but on the JVM.
```text
VmRSS:    183580 kB
```


In this specific case, it’s about 1⁄10 of memory usage.

Application size
----------------

The application’s fat jar is 5.7MB large and the smallest JRE is 57MB: [https://hub.docker.com/r/library/openjdk/tags/](https://hub.docker.com/r/library/openjdk/tags/). For simplicity, let’s say 60MB total. The native binary is about 22MB large:
```text
-rwxr-xr-x 1 j0e users  22M Sep 24 01:38 graal-javalin
```

That’s ~ 1⁄3 the size. That’s absolutely acceptable, and almost in range of the size of Go binaries. Please note that with JDK9, sizes may be smaller. So I think the advantage here exists, but may not be very large.

In general, GraalVM is a cool thing. It just feels like a dirty hack. I really dislike that there may always be runtime errors that GraalVM can’t predict at compile time (correct me if I'm wrong). I’m not sure if this is the future of Java, but does it have one anyway? ;) It’s worth noting that some library/framework authors are actively investing time into supporting GraalVM. As a matter of fact, micronaut.io is now compatible: [https://github.com/graemerocher/micronaut-graal-experiments](https://github.com/graemerocher/micronaut-graal-experiments).

The full code, including a Dockerfile is available on GitHub: [https://github.com/birdayz/graal-javalin](https://github.com/birdayz/graal-javalin).

In addition, I made a Docker image you can use as base image to build a container with only the static executable, similar how it is done for Go applications.


```text
FROM birdy/graalvm:latest
WORKDIR /tmp/build
ENV GRADLE_USER_HOME /tmp/build/.gradle

ADD . /tmp/build
RUN ./gradlew build fatJar
RUN native-image -jar /tmp/build/build/libs/graal-javalin-all-1.0-SNAPSHOT.jar -H:ReflectionConfigurationFiles=reflection.json -H:+JNI \
  -H:Name=graal-javalin --static --delay-class-initialization-to-runtime=io.javalin.json.JavalinJson

FROM scratch
COPY --from=0 /tmp/build/graal-javalin /
ENTRYPOINT ["/graal-javalin"]
```

This Dockerfile uses Docker multi-stage builds. There are two containers: one just used for the build, and the final output container which only contains the application.
