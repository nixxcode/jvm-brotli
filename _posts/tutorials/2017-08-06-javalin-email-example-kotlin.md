---
layout: tutorial
title: "Creating a 'Contact us' form with email-sending (via gmail)"
author: <a href="https://www.linkedin.com/in/davidaase" target="_blank">David Åse</a>
date: 2017-08-06
permalink: /tutorials/email-sending-example-kotlin
github: https://github.com/tipsy/javalin-email-example
summarytitle: Sending emails from a Kotlin backend
summary: Learn how to create a 'Contact us' form with email sending (via gmail) with a Kotlin backend
language: kotlin
---

## Dependencies

First, we need to create a Gradle project with some dependencies: [(→ Tutorial)](/tutorials/gradle-setup)

~~~java
dependencies {
    compile "io.javalin:javalin:{{site.javalinversion}}"
    compile "org.apache.commons:commons-email:1.4"
    compile "org.slf4j:slf4j-simple:{{site.slf4jversion}}"
}
~~~

## Setting up the backend
We need three endpoints: `GET '/'`, `POST '/contact-us'` and `GET '/contact-us/success'`:

```kotlin
import io.javalin.Javalin
import org.apache.commons.mail.*

fun main(args: Array<String>) {

    val app = Javalin.create().start(7000)

    app.get("/") { ctx ->
        ctx.html("""
                <form action="/contact-us" method="post">
                    <input name="subject" placeholder="Subject">
                    <br>
                    <textarea name="message" placeholder="Your message ..."></textarea>
                    <br>
                    <button>Submit</button>
                </form>
        """.trimIndent())
    }

    app.post("/contact-us") { ctx ->
        SimpleEmail().apply {
            setHostName("smtp.googlemail.com")
            setSmtpPort(465)
            setAuthenticator(DefaultAuthenticator("YOUR_EMAIL", "YOUR_PASSWORD"))
            setSSLOnConnect(true)
            setFrom("YOUR_EMAIL")
            setSubject(ctx.formParam("subject"))
            setMsg(ctx.formParam("message"))
            addTo("RECEIVING_ADDRESS")
        }.send() // will throw email-exception if something is wrong
        ctx.redirect("/contact-us/success")
    }

    app.get("/contact-us/success") { ctx -> ctx.html("Your message was sent") }

}
```

In order to get the above code to work, you need to make some changes:

* Change `YOUR_EMAIL` to your gmail account <small>(youremail@gmail.com)</small>
* Change `YOUR_PASSWORD` to your gmail password*
* Change `RECEIVING_ADDRESS` to where you want the email to be sent

<small>**It might be a good idea to create a test-account instead of using your real gmail credentials.*</small>

When you've made the changes to the code, run the program and go to `http://localhost:7000`.
You will see a simple unstyled form with an input field, a textarea and a button.
Fill in the form and click the button to test your email server. After you click the button, your browser
is redirected to `/contact-us/success` (if the email was sent).

Any emails you have sent will show up in your `Sent` folder in the gmail web-interface.
