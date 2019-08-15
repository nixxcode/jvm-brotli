---
layout: tutorial
title: "Setting up Javalin with Gradle"
author: <a href="https://www.linkedin.com/in/davidaase" target="_blank">David Åse</a>
date: 2017-05-24
permalink: /tutorials/gradle-setup
summarytitle: Gradle setup
summary: Set up a Javalin project using Gradle in IntelliJ IDEA
language: kotlin
---

When you're done with this tutorial, your `build.gradle` file
should look like this:

~~~java
group 'io.javalin' // your group id
version '1.0-SNAPSHOT'

apply plugin: 'java'

sourceCompatibility = 1.8

repositories {
    mavenCentral()
}

dependencies {
    compile 'io.javalin:javalin:{{site.javalinversion}}'
    testCompile group: 'junit', name: 'junit', version: '4.12'
}
~~~

<h2 id="intellij">Step by step instructions</h2>

* `File` `->` `New` `->` `Project`
* Select `Gradle` then `Java` (or `Kotlin`), click `Next`
* Enter `groupId` and `artifactId`, click `Next`
* Check `Use auto-import`, click `Next`

Open the newly generated `build.gradle` file and add the gradle-dependency \\
`compile 'io.javalin:javalin:{{site.javalinversion}}'` to the `dependencies {}` scope.
See the full `build.gradle` example above if you're not sure where to put it.

Finally, create a file `src/main/java/HelloWorld.java` or `src/main/kotlin/HelloWorld.kt`\\
and paste the Hello World example:

{% include macros/gettingStarted.md %}

Now everything is ready for you to start programming. Enjoy!
