---
layout: tutorial
title: "Working with HTML forms and a Kotlin backend"
author: <a href="https://www.linkedin.com/in/davidaase" target="_blank">David Åse</a>
date: 2017-07-28
permalink: /tutorials/html-forms-example-kotlin
github: https://github.com/tipsy/javalin-html-forms-example
summarytitle: HTML forms & Kotlin backend
summary: Learn how to get/post HTML forms to a Kotlin backend
language: kotlin
---

## Dependencies

First, we need to create a project with these dependencies: [(→ Tutorial)](/tutorials/gradle-setup)

~~~java
dependencies {
    compile "io.javalin:javalin:{{site.javalinversion}}"
    compile "org.slf4j:slf4j-simple:{{site.slf4jversion}}"
    compile "commons-fileupload:commons-fileupload:1.3.3"
}
~~~

## Setting up the backend

Create a Kotlin file, for example `Main.kt`, that has the following code:

```kotlin
import io.javalin.Javalin
import org.apache.commons.io.FileUtils
import java.io.File

val reservations = mutableMapOf<String?, String?>(
        "saturday" to "No reservation",
        "sunday" to "No reservation"
)

fun main(args: Array<String>) {

    val app = Javalin.create {
        it.addStaticFiles("/public")
    }.start(7777)

    app.post("/make-reservation") { ctx ->
        reservations[ctx.formParam("day")] = ctx.formParam("time")
        ctx.html("Your reservation has been saved")
    }

    app.get("/check-reservation") { ctx ->
        ctx.html(reservations[ctx.queryParam("day")]!!)
    }

}
```

This will create an app which listens on port `7777`,
and looks for static files in your `/src/resources/public` folder.
We have three endpoints mapped, two `post`s for uploading data, and one `get` for retrieving data we've stored.

## Reservation example

We have to make HTML forms for interacting with our endpoints.
We can put these forms in a file `/resources/public/index.html`, which will be
available at `http://localhost:7777/`.

### Make reservation form
```markup
<h2>Make reservation:</h2>
<form method="post" action="/make-reservation">
    Choose day
    <select name="day">
        <option value="saturday">Saturday</option>
        <option value="sunday">Sunday</option>
    </select>
    <br>
    Choose time
    <select name="time">
        <option value="8:00 PM">8:00 PM</option>
        <option value="9:00 PM">9:00 PM</option>
    </select>
    <br>
    <button>Submit</button>
</form>
```

To make a reservation we need to create something on the server
(in this case it's a simple `map.put()`, but usually you'd have a database).
When creating something on the server, you should use the `POST` method,
which can be specified by adding `method="post"` to the `<form>` element.

In our Kotlin code, we have a post endpoint: `app.post("/make-reservation") {...}`. We
need to tell our form to use this endpoint with the action attribute: `action="/make-reservation"`.
Actions are relative, so when you click submit, the browser will create a `POST` request
to `http://localhost:7777/make-reservation` with the `day`/`time` values as the request-body.

### Check reservation form
```markup
<h2>Check your reservation:</h2>
<form method="get" action="/check-reservation">
    Choose day
    <select name="day">
        <option value="saturday">Saturday</option>
        <option value="sunday">Sunday</option>
    </select>
    <br>
    <button>Submit</button>
</form>
```

To check a reservation we need to tell the server which day we're interested in.
In this case we're not creating anything, and our action does not change the state
of the server in any way, which makes it a good candidate for a `GET` request.

`GET` requests don't have a request-body so when you click submit the browser
creates a `GET` request to `http://localhost:7777/check-reservation?day=saturday`.
The values of the form are added to the URL as query-parameters.

### HTML form GET vs POST summary
* `POST` requests should be used if the request can change the server state.
* `POST` requests have their information stored in the request-body. In order to extract information from this body you have to use `ctx.formParam(key)` in Javalin.
* Performing a series of `GET` requests should always return the same result (if no other `POST` request was performed in-between).
* `GET` requests have no request-body, and form information is sent as query-parameters in the URL. In order to extract information from this body you have to use `ctx.queryParam(key)` in Javalin.

## File upload example
Let's expand our example a bit to include file uploads.

### Endpoint
```kotlin
app.post("/upload-example") { ctx ->
    ctx.uploadedFiles("files").forEach {
        FileUtils.copyInputStreamToFile(it.content, File("upload/" + ${it.filename}))
    }
    ctx.html("Upload complete")
}
```

The `ctx.uploadedFiles("files")` method gives us a list of files matching the name `files`.
We then save these files to an `upload` folder.

### HTML form

```markup
<h1>Upload example</h1>
<form method="post" action="/upload-example" enctype="multipart/form-data">
    <input type="file" name="files" multiple>
    <button>Submit</button>
</form>
```

When uploading files you need to add `enctype="multipart/form-data"` to your `<form>`.
If you want to upload multiple files, add the `multiple` attribute to your `<input>`.
