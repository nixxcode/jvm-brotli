---
layout: tutorial
title: "Using Javalin with Kotlin to create a simple CRUD REST API"
author: <a href="https://www.linkedin.com/in/davidaase" target="_blank">David Åse</a>
date: 2017-05-25
permalink: /tutorials/simple-kotlin-example
github: https://github.com/tipsy/javalin-kotlin-example
summarytitle: Kotlin CRUD REST API
summary: Use Kotlin with Javalin to create a simple CRUD REST API.
language: kotlin
---

## What You Will Learn

* Setting up Kotlin with Maven
* Creating a Javalin/Kotlin CRUD REST API (no database)
* Some neat Kotlin features

The instructions for this tutorial will focus on IntelliJ IDEA,
as it's made by JetBrains, the same people who make Kotlin.
We recommend downloading the free [community edition](https://www.jetbrains.com/idea/download)
of IDEA while following this tutorial, but there is also Kotlin support in Eclipse.

## Setting up Kotlin with Maven (in IntelliJ IDEA)

The good people over at [JetBrains](https://www.jetbrains.com) have an up-to-date
[archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html)
for Kotlin. To use it, do as follows:
 
 * `File` `->` `New` `->` `Project`
 * `Maven` `->` `Create from archetype` `->` `org.jetbrains.kotlin:kotlin-archetype-jvm` `->` `Next`
 * Follow the instructions and pick a project name
 * Create `src/main/kotlin/app/Main.kt`
 
 There is no `public static void main(String[] args)` in Kotlin, instead you have a `fun main(args: Array<String>)`.
 
~~~kotlin
fun main(args: Array<String>) {
    println("Hello, world!")
}
~~~
 
<div class="comment">
You'll have to point to the file (not class) containing this main function (not method)
from your pom.xml if you want to build a jar. Doing this is not necessary for this tutorial,
but the code on GitHub demonstrates how to do it for those interested.
</div>

## Using Javalin with Kotlin

Add the dependency:

{% include macros/mavenDep.md %}

And paste the "Hello world" example:

~~~kotlin
import io.javalin.Javalin

fun main(args: Array<String>) {
    val app = Javalin.create().start(7000)
    app.get("/") { ctx -> ctx.result("Hello World") }
}
~~~

It looks pretty similar to Java8:
<br>
Java8: `get("/path", ctx -> { ... });`
<br>
Kotlin: `get("/path") { ctx -> ...}`.

The syntax `(){}` might look a little strange to Java programmers.
Kotlin supports [trailing closures](https://kotlinlang.org/docs/reference/lambdas.html#closures)
and provides [semicolon inference](https://kotlinlang.org/docs/reference/grammar.html#semicolons).
Simplified, this means you don't have to wrap closures in parentheses and end statements with semicolons.

## Creating a Javalin/Kotlin CRUD microservice

### Kotlin data-classes

Kotlin has a really neat feature called
[Data classes](https://kotlinlang.org/docs/reference/data-classes.html).
To create a data class you just have to write:

~~~kotlin
data class User(val name: String, val email: String, val id: Int)
~~~

... and you're done! If you declare all parameters as `val` you get an immutable class similar to the
[Lombok @Value](https://projectlombok.org/features/Value.html) annotation, only better.
Regardless of if you use `var` or `val` (or a mix) for your data class,
you get toString, hashCode/equals, copying and destructuring included:

~~~kotlin
val alice = User(name = "Alice", email = "alice@alice.kt", id = 0)
val aliceNewEmail = alice.copy(email = "alice@bob.kt") // new object with only email changed

val (name, email) = aliceNewEmail // choose the fields you want
println("$name's new email is $email") // prints "Alice's new email is alice@bob.kt"
~~~

### Initializing some data
Let's initialize our fake user-database with four users:

~~~kotlin
val users = hashMapOf(
    0 to User(name = "Alice", email = "alice@alice.kt", id = 0),
    1 to User(name = "Bob", email = "bob@bob.kt", id = 1),
    2 to User(name = "Carol", email = "carol@carol.kt", id = 2),
    3 to User(name = "Dave", email = "dave@dave.kt", id = 3)
)
~~~

Kotlin has type inference and named parameters (we could have written our arguments in any order).
It also has a nice standard library providing map-literal-like functions (so you won't have to include guava in every project).

### Creating a data access object
We need to be able to read out data somehow, so let's set up some
basic CRUD functionality, with one added function for finding user by email:

~~~kotlin
class UserDao {

    val users = hashMapOf(
        0 to User(name = "Alice", email = "alice@alice.kt", id = 0),
        1 to User(name = "Bob", email = "bob@bob.kt", id = 1),
        2 to User(name = "Carol", email = "carol@carol.kt", id = 2),
        3 to User(name = "Dave", email = "dave@dave.kt", id = 3)
    )

    var lastId: AtomicInteger = AtomicInteger(users.size - 1)

    fun save(name: String, email: String) {
        val id = lastId.incrementAndGet()
        users.put(id, User(name = name, email = email, id = id))
    }

    fun findById(id: Int): User? {
        return users[id]
    }

    fun findByEmail(email: String): User? {
        return users.values.find { it.email == email }
    }

    fun update(id: Int, user: User) {
        users.put(id, User(name = user.name, email = user.email, id = id))
    }

    fun delete(id: Int) {
        users.remove(id)
    }

}
~~~

The `findByEmail` function shows of some neat features. In addition to the
trailing closures that we saw earlier, Kotlin also has a very practical `find` function
and a special `it` keyword, which replaces `user -> user` style declarations with just `it`
([docs](https://kotlinlang.org/docs/reference/lambdas.html#it-implicit-name-of-a-single-parameter)).
The function also demonstrates that `==` is the structural equality operator for Strings in Kotlin
(equivalent to `.equals()` in Java). If you want to check for referential equality in Kotlin you can use `===`.
Another thing worth noticing is that the find-functions return `User?`, which means the function will
return either a `User` or `null`. In Kotlin you have to specify the possibility of a null-return.

`findByEmail()`, Kotlin vs Java:

~~~kotlin
// Kotlin 
fun findByEmail(email: String): User? {
    return users.values.find { it.email == email }
}

// Java
public User findByEmail(String email) {
    return users.values().stream()
            .filter(user -> user.getEmail().equals(email))
            .findFirst()
            .orElse(null);
}
~~~

### Creating the REST API

Kotlin and Javalin play very well together (in fact, Kotlin seems to play well with all Java dependencies).
We can use `with` ([docs](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/with.html))
and trailing closures to create very clean api declarations:

~~~kotlin
import app.user.User
import app.user.UserDao
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.Javalin

fun main(args: Array<String>) {

    val userDao = UserDao()

    val app = Javalin.create().apply {
        exception(Exception::class.java) { e, ctx -> e.printStackTrace() }
        error(404) { ctx -> ctx.json("not found") }
    }.start(7000)

    app.routes {

        get("/users") { ctx ->
            ctx.json(userDao.users)
        }

        get("/users/:user-id") { ctx ->
            ctx.json(userDao.findById(ctx.pathParam("user-id").toInt())!!)
        }

        get("/users/email/:email") { ctx ->
            ctx.json(userDao.findByEmail(ctx.pathParam("email"))!!)
        }

        post("/users") { ctx ->
            val user = ctx.body<User>()
            userDao.save(name = user.name, email = user.email)
            ctx.status(201)
        }

        patch("/users/:user-id") { ctx ->
            val user = ctx.body<User>()
            userDao.update(
                    id = ctx.pathParam("user-id").toInt(),
                    user = user
            )
            ctx.status(204)
        }

        delete("/users/:user-id") { ctx ->
            userDao.delete(ctx.pathParam("user-id").toInt())
            ctx.status(204)
        }

    }

}
~~~

## Conclusion
I had only worked with Kotlin for a few hours before writing this tutorial,
but I'm already a very big fan of the language. Everything just seems to make sense, and the interoperability with Java is great.
IntelliJ will also automatically convert Java code into Kotlin if you paste it into your project.
Please clone the repo and give it a try!
