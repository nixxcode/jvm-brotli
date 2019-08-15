---
layout: tutorial
title: "Creating a secure REST API in Javalin"
author: <a href="https://www.linkedin.com/in/davidaase" target="_blank">David Åse</a>
date: 2017-08-24
permalink: /tutorials/auth-example
github: https://github.com/tipsy/javalin-auth-example
summarytitle: Secure your endpoints!
summary: Learn how to secure your endpoints using Javalin's built-in AccessManager
language: kotlin
---

## Dependencies

First, create a new Gradle project with the following dependencies: [(→ Tutorial)](/tutorials/gradle-setup)

~~~java
dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlin_version"
    compile "io.javalin:javalin:{{site.javalinversion}}"
    compile "com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9"
    compile "org.slf4j:slf4j-simple:{{site.slf4jversion}}"
}
~~~

## Creating controllers
We need something worth protecting.
Let's pretend we have a very important API for manipulating a user database.
We make a controller-object with some dummy data and CRUD operations:
```kotlin
import io.javalin.http.Context
import java.util.*

object UserController {

    private data class User(val name: String, val email: String)

    private val users = hashMapOf(
            randomId() to User(name = "Alice", email = "alice@alice.kt"),
            randomId() to User(name = "Bob", email = "bob@bob.kt"),
            randomId() to User(name = "Carol", email = "carol@carol.kt"),
            randomId() to User(name = "Dave", email = "dave@dave.kt")
    )

    fun getAllUserIds(ctx: Context) {
        ctx.json(users.keys)
    }

    fun createUser(ctx: Context) {
        users[randomId()] = ctx.body<User>()
    }

    fun getUser(ctx: Context) {
        ctx.json(users[ctx.pathParam(":user-id")]!!)
    }

    fun updateUser(ctx: Context) {
        users[ctx.pathParam(":user-id")!!] = ctx.body<User>()
    }

    fun deleteUser(ctx: Context) {
        users.remove(ctx.pathParam(":user-id"))
    }

    private fun randomId() = UUID.randomUUID().toString()

}
```

<small><em>We're using `!!` to convert nullables to non-nullables.
If `:user-id` is missing or `users[id]` returns null, we'll get a NullPointerException
and our application will crash. Handling this is outside the scope of the tutorial.</em></small>

## Creating roles
Now that we have our functionality, we need to define a set of roles for our system.
This is done by implementing the `Role` interface from `io.javalin.security.Role`.
We'll define three roles, one for "anyone", one for permission to read user-data,
and one for permission to write user-data.

```kotlin
enum class ApiRole : Role { ANYONE, USER_READ, USER_WRITE }
```

## Setting up the API
Now that we have roles, we can implement our endpoints:

```kotlin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.Javalin
import io.javalin.core.security.Role.roles

fun main(vararg args: String) {

    val app = Javalin.create {
        it.accessManager(Auth::accessManager)
    }.start()

    app.routes {
        path("users") {
            get(UserController::getAllUserIds, roles(ApiRole.ANYONE))
            post(UserController::createUser, roles(ApiRole.USER_WRITE))
            path(":user-id") {
                get(UserController::getUser, roles(ApiRole.USER_READ))
                patch(UserController::updateUser, roles(ApiRole.USER_WRITE))
                delete(UserController::deleteUser, roles(ApiRole.USER_WRITE))
            }
        }
    }

}
```

A role has now been given to every endpoint:
* `ANYONE` can `getAllUserIds`
* `USER_READ` can `getUser`
* `USER_WRITE` can `createUser`, `updateUser` and `deleteUser`

Now, all that remains is to implement the access-manager (`Auth::accessManager`).

## Implementing auth

The `AccessManager` interface in Javalin is pretty simple.
It takes a `Handler` a `Context` and a set of `Role`s.
The idea is that you implement code to run the handler
based on what's in the context, and what roles are set for the endpoint.

The rules for our access manager are also simple:
* When endpoint has `ApiRole.ANYONE`, all requests will be handled
* When endpoint has another role set and the request has matching credentials, the request will be handled
* Else we ignore the request and send `401 Unauthorized` back to the client

This translates nicely into Kotlin:
```kotlin
fun accessManager(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
    when {
        permittedRoles.contains(ApiRole.ANYONE) -> handler.handle(ctx)
        ctx.userRoles.any { it in permittedRoles } -> handler.handle(ctx)
        else -> ctx.status(401).json("Unauthorized")
    }
}
```

### Extracting user-roles from the context
There is no `ctx.userRoles` concept in Javalin, so we need to implement it.
First we need a user-table. We'll create a `map(Pair<String, String>, Set<Role>)` where keys are
username+password in cleartext (please don't do this for a real service), and values are user-roles:

```kotlin
private val userRoleMap = hashMapOf(
        Pair("alice", "weak-password") to setOf(ApiRole.USER_READ),
        Pair("bob", "better-password") to setOf(ApiRole.USER_READ, ApiRole.USER_WRITE)
)
```

Now that we have a user-table, we need to authenticate the requests.
We do this by getting the username+password from the [Basic-auth-header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication#Basic_authentication_scheme)
and using them as keys for the `userRoleMap`:

```kotlin
private val Context.userRoles: Set<ApiRole>
    get() = this.basicAuthCredentials()?.let { (username, password) ->
        userRoleMap[Pair(username, password)] ?: setOf()
    } ?: setOf()
```

<small><em>
When using basic auth, credentials are transferred as plain text (although base64-encoded).
**Remember to enable SSL if you're using basic-auth for a real service.**
</em></small>

## Conclusion
This tutorial showed one possible way of implementing an `AccessManager` in Javalin, but
the interface is very flexible and you can really do whatever you want:
```kotlin
app.accessManager(handler, ctx, permittedRoles) -> {
    when {
        ctx.host().contains("localhost") -> handler.handle(ctx)
        Math.random() > 0.5 -> handler.handle(ctx)
        dayOfWeek == Calendar.SUNDAY -> handler.handle(ctx)
        else -> ctx.status(401).json("Unauthorized")
    }
};
```
