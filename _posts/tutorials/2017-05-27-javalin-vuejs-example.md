---
layout: tutorial
title: "TODO MVC with Vue.js and Kotlin"
author: <a href="https://www.linkedin.com/in/davidaase" target="_blank">David Åse</a>
date: 2017-05-27
permalink: /tutorials/kotlin-vuejs-example
github: https://github.com/tipsy/javalin-vuejs-example
summarytitle: Single-page app with Kotlin and Vue.js
summary: Use Vue.js and Kotlin to create the famous TODO MVC app
language: kotlin
---

## This is a very short tutorial

If you need to learn how to setup Kotlin with Maven, please
follow the beginning of our [Kotlin CRUD REST API tutorial](/tutorials/simple-kotlin-example)

[A live demo can be found here](http://javalin-vuejs-example.herokuapp.com)

## Dependencies
~~~markup
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>{{site.javalinversion}}</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>{{site.slf4jversion}}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.module</groupId>
    <artifactId>jackson-module-kotlin</artifactId>
    <version>2.9.9</version>
</dependency>
~~~

## Our main class

~~~kotlin
fun main(args: Array<String>) {

    var todos = arrayOf(Todo(123123123, "My very first todo", false))

    val app = Javalin.create {
        it.addStaticFiles("/public")
    }.start(7000)

    app.routes {
        get("/todos") { ctx ->
            ctx.json(todos)
        }
        put("/todos") { ctx ->
            todos = ctx.body<Array<Todo>>()
            ctx.status(204)
        }
    }

}
~~~

We're use Javalin to serve our static files, as well as
handle two endpoints: `get` and `put`.

Most of the work here is being done by `ctx.json` and `ctx.bodyAsClass`,
which map a Todo data-class:

~~~kotlin
data class Todo(val id: Long = -1, val title: String = "", val completed: Boolean = false)
~~~

That's it. The rest of the logic is in `index.html` (vue template)
and `todomvc.js` (vue logic). \\
This is not a JavaScript tutorial, so please have a look at those files for yourself.
