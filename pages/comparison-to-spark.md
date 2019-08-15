---
layout: default
title: Comparison to SparkJava
rightmenu: false
permalink: /comparisons/sparkjava
---

<h1 class="no-margin-top">SparkJava and Javalin comparison</h1>
People often ask about the differences between Spark and Javalin.
Javalin originally started as a fork of Spark, and only covered a subset of Spark's functionality.
The project became popular very fast, and reached feature parity with Spark within months.

Javalin now offers a lot of features that are not available in Spark:

* Fully configurable Jetty (HTTP2)
* OpenAPI (Swagger) support
* Async request handling
* Lambda WebSockets with routing (path params)
* Server-sent events
* Session handling
* Input validation and casting
* Default responses
* Access management
* Error mapping (status code)
* Lifecycle events
* Extensions
* Handler groups
* Simplified uploads
* Multi-location static files
* Single-page mode
* Stand-alone mode (you can add Javalin to any Servlet container programmatically)
* Plugins for JSON and templates

## Syntax differences

Javalin has the concept of a **Handler**. A **Handler** is void and takes a **Context**, which wraps **HttpServletRequest** and **HttpServletResponse**. You operate on both the request and response through this **Context**.

```java
javalin.get("/path", ctx -> ctx.result("Hello, World!"));
javalin.after("/path", ctx -> ctx.result("Actually, nevermind..."));
```

Spark on the other hand has **Routes** and **Filters** . Both **Route** and **Filter** in Spark take
**(Request, Response)** as input. **Route** has a return value (**Object**), while **Filter** is void.

**Request** and **Response** wrap **HttpServletRequest** and **HttpServletResponse**, respectively.

```java
Spark.get("/path", (req, res) -> "Hello, World!");
Spark.after("/path", (req, res) -> res.body("Actually, nevermind..."));
```

This means that in Javalin you always call `ctx.result(string)` when you want to update the result,
whereas in Spark you have to `return string` in **Routes** and use `response.body(string)` in **Filters**.

Javalin's approach leads to a much more consistent API, both for the previous and the next examples:

### Redirects
```java
javalin.get("/", ctx -> ctx.redirect("/new-path"));
// vs
Spark.get("/", (req, res) -> {
    res.redirect("/new-path"); // can't return here, the redirect method is void
    return ""; // if you return null here you get a 404
});
```

### JSON mapping
```java
javalin.get("/", ctx -> ctx.json(object));
// vs
Spark.get("/", (req, res) -> object, new JsonTransformer());
```

### Templates

```java
javalin.get("/", ctx -> ctx.render("path", model));
// vs
Spark.get("/", (req, res) -> new ModelAndView(model, "path"), new TemplateEngine());
```

## Code base and performance

Javalin's code-base is a lot smaller than Spark's (about one third the size) and written in Kotlin. All methods have proper nullable types.
There are hundreds of tests (there is more test code than actual code).
Javalin is about twice as fast, according to the
[TechEmpower Benchmarks](https://www.techempower.com/benchmarks/#section=test&runid=a0d523de-091b-4008-b15d-bd4c8aa25066&hw=ph&test=plaintext&l=xan9tr-3&a=2).
