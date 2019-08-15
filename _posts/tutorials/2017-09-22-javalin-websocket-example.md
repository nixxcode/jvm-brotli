---
layout: tutorial
title: "Creating a simple chat-app with WebSockets"
author: <a href="https://www.linkedin.com/in/davidaase" target="_blank">David Åse</a>
date: 2017-09-22
permalink: /tutorials/websocket-example
github: https://github.com/tipsy/javalin-websocket-example
summarytitle: WebSockets chat application
summary: Learn how to create a simple chat-app with WebSockets in Java
language: java
---

A live demo of this app can be found [here](http://javalin-websocket-example.herokuapp.com) (loads slowly first time)

## What You Will Learn
In this tutorial we will create a simple real-time chat application.
It will feature a chat-panel that stores messages received after you join,
a list of currently connected users, and an input field to send messages from.
We will be using WebSockets for this, as WebSockets provides us with full-duplex
communication channels over a single TCP connection, meaning we won't have to
make additional HTTP requests to send and receive messages.
A WebSocket connection stays open, greatly reducing latency (and complexity).

## Dependencies

First, we need to create a Maven project with some dependencies: [(→ Tutorial)](/tutorials/maven-setup)

~~~xml
<dependencies>
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
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20160810</version>
    </dependency>
    <dependency>
        <groupId>com.j2html</groupId>
        <artifactId>j2html</artifactId>
        <version>1.4.0</version>
    </dependency>
</dependencies>
~~~

## The Java application
The Java application is pretty straightforward.
We need:
 * a map to keep track of session/username pairs.
 * a counter for number of users (nicknames are auto-incremented)
 * websocket handlers for connect/message/close
 * a method for broadcasting a message to all users
 * a method for creating the message in HTML (or JSON if you prefer)

```java
public class Chat {

    private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
    private static int nextUserNumber = 1; // Assign to username for next connecting user

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("/public");
        }).start(HerokuUtil.getHerokuAssignedPort());

        app.ws("/chat", ws -> {
            ws.onConnect(ctx -> {
                String username = "User" + nextUserNumber++;
                userUsernameMap.put(ctx, username);
                broadcastMessage("Server", (username + " joined the chat"));
            });
            ws.onClose(ctx -> {
                String username = userUsernameMap.get(ctx);
                userUsernameMap.remove(ctx);
                broadcastMessage("Server", (username + " left the chat"));
            });
            ws.onMessage(ctx -> {
                broadcastMessage(userUsernameMap.get(ctx), ctx.message());
            });
        });
    }

    // Sends a message from one user to all users, along with a list of current usernames
    private static void broadcastMessage(String sender, String message) {
        userUsernameMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> {
            session.send(
                new JSONObject()
                    .put("userMessage", createHtmlMessageFromSender(sender, message))
                    .put("userlist", userUsernameMap.values()).toString()
            );
        });
    }

    // Builds a HTML element with a sender-name, a message, and a timestamp
    private static String createHtmlMessageFromSender(String sender, String message) {
        return article(
            b(sender + " says:"),
            span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
            p(message)
        ).render();
    }

}
```

## Building a JavaScript Client
In order to demonstrate that our application works, we can build a JavaScript client.
First we create our index.html:

```markup
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>WebsSockets</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div id="chatControls">
        <input id="message" placeholder="Type your message">
        <button id="send">Send</button>
    </div>
    <ul id="userlist"> <!-- Built by JS --> </ul>
    <div id="chat">    <!-- Built by JS --> </div>
    <script src="websocketDemo.js"></script>
</body>
</html>
```

As you can see, we reference a stylesheet called style.css, which can be found on
[GitHub](https://github.com/tipsy/javalin-websocket-example/blob/master/src/main/resources/public/style.css).

The final step needed for completing our chat application is creating `websocketDemo.js`:

```javascript
// small helper function for selecting element by id
let id = id => document.getElementById(id);

//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/chat");
ws.onmessage = msg => updateChat(msg);
ws.onclose = () => alert("WebSocket connection closed");

// Add event listeners to button and input field
id("send").addEventListener("click", () => sendAndClear(id("message").value));
id("message").addEventListener("keypress", function (e) {
    if (e.keyCode === 13) { // Send message if enter is pressed in input field
        sendAndClear(e.target.value);
    }
});

function sendAndClear(message) {
    if (message !== "") {
        ws.send(message);
        id("message").value = "";
    }
}

function updateChat(msg) { // Update chat-panel and list of connected users
    let data = JSON.parse(msg.data);
    id("chat").insertAdjacentHTML("afterbegin", data.userMessage);
    id("userlist").innerHTML = data.userlist.map(user => "<li>" + user + "</li>").join("");
}
```

And that's it! Now try opening `localhost:7070` in a couple of different
browser windows (that you can see simultaneously) and talk to yourself.

## Conclusion
Well, that was easy! We have a working real-time chat application implemented without polling,
written in a total of less than 100 lines of Java and JavaScript.
The implementation is very basic though, and we should at least split up the sending of the userlist
and the messages (so that we don't rebuild the user list every time anyone sends a message),
but since the focus of this tutorial was supposed to be on WebSockets,
I chose to do the implementation as minimal as I could be comfortable with.
