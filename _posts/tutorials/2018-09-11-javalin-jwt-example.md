---
layout: tutorial
title: "Using JWT with a Javalin application"
author: <a href="https://github.com/kmehrunes" target="_blank">Khaled Y.M.</a>
date: 2018-09-11
summarytitle: JWT in a Javalin Application
summary: Learn how to use JWT with Javalin
language: java
---

This is a simple tutorial on how to integrate JWT into a Javalin application. 
It relies on an extension which can be found [here](https://github.com/kmehrunes/javalin-jwt).

## What You Will Learn
In this tutorial we will introduce the extension and what it provides, then we will 
show a basic use, and finally we will go a bit deeper and use some components 
provided by the extension. The tutorial assumes that you know what JWTs are. If you 
do not, then you can check [this post](https://jwt.io/introduction/) for an easy but 
quite thorough introduction.

## Dependencies
Currently there is no Maven dependency to get the extension directly; you need to 
pull the source code. There should be one ready soon, and this tutorial will be 
updated accordingly.

The extension itself depends on [Auth0 Java JWT library](https://github.com/auth0/java-jwt).

## The Extension
**Note: it is recommended that you familiarize yourself with Auth0 Java JWT first**
The extension itself is quite small, and it provides three things:
- Helper functions for Javalin Context to make working with JWTs easier, 
includes: extracting tokens from authorization headers, adding/getting tokens 
to/from cookies, and adding decoded JWT objects to contexts for future handlers 
to use

- Decode helpers which take care of extracting, validating, and adding decoded 
objects to the context for you

- An access manager

There is no requirement to use all parts of the extension, you can use only 
the parts you need for your particular case.

## Preliminary Steps
For any use of the extension, we need what we call a JWT provider (for lack of
a better word). A provider is a somewhat convient way of working with JWT which 
wraps a generator and a verifier. Where A generator implements the functional 
interface JWTGeneratr, and a verifier which is the normal Auth0 JWTVerifier.

Before being able to create a provider, we first need to have: a user class,
a generator, and a verifier. For the sake of this tutorial we will assume the 
following class as our user class:
```java
class MockUser {
    String name;
    String level;

    MockUser(String name, String level) {
        this.name = name;
        this.level = level;
    }
}
```

Now we can create our JWT provider as follows:
```java
//1.
Algorithm algorithm = Algorithm.HMAC256("very_secret");

//2.
JWTGenerator<MockUser> generator = (user, alg) -> {
            JWTCreator.Builder token = JWT.create()
                    .withClaim("name", user.name)
                    .withClaim("level", user.level);
            return token.sign(alg);
        };

//3.
JWTVerifier verifier = JWT.require(algorithm).build();

//4.
JWTProvider provider = JWTProvider(algorithm, generator, verifier);
```
1) First we initialize the algorithm we are going to use. In our 
case we chose HMAC256 but feel free to try other variants. Tip: 
if you are separating the application into services where only 
one service will issue the tokens then consider using an asymetric 
algorithm like RSA.

2) In the second step we create our JWT generator. It implements 
a function which takes an object and algorithm to generate a token 
with a set of claims and returns the token signed.

3) In the third step we create a verifier using the builder 
provided by Auth0. In our case we only have the algorithm 
but there are many more to be added depending on your case.

4) We finally create our provider which we will use throughout the 
rest of this tutorial.

## Basic Example
Now that we have everything ready, we can finally start using 
the provider in our application. We will create a simple application 
which only has two routes: */generate* and */validate*.

```java
//
// .. create your Javalin app ...
//
Handler generateHandler = context -> {
    MockUser mockUser = new MockUser("Mocky McMockface", "user");
    String token = provider.generateToken(mockUser);
    context.json(new JWTResponse(token));
};

Handler validateHandler = context -> {
    Optional<DecodedJWT> decodedJWT = JavalinJWT.getTokenFromHeader(context)
                                                  .flatMap(provider::validateToken);

    if (!decodedJWT.isPresent()) {
        context.status(401).result("Missing or invalid token");
    }
    else {
        context.result("Hi " + decodedJWT.get().getClaim("name").asString());
    }
};

app.get("/generate", generateHandler);
app.get("/validate", validateHandler);
```

In *generateHandler* we use the provider to generate a token for a mock user.
We then wrap that token in a response object which will result in the JSON 
response object to be {"jwt": "..."}. In a real world example, that object 
might also contain a renewal token, an expiry date..etc.

In *validateHandler* we extract the token from authorization header using 
one of the provided helper functions, then we validate it using the provider.
The authorization header value must have a Bearer scheme. An empty option
represents lack of a token value or an invalid token.

Now if you visit /generate, you'll get a JWT for the created user. Then you 
need to put that token in an authorization header with "Bearer" scheme and 
issue a request to /validate.

Some links to check:
- [Authorization header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization)
- [Bearer scheme](https://tools.ietf.org/html/rfc6750)

## Advanced Example
The previous example showed the basic functionality of the extension but you 
might have noticed two impracticalities in the implementation: you need to 
handle extracting and verifying the JWT for every handler that needs it, and 
you need to perform access control inside every handler. In this example we 
will show how we can solve those two problems using decode handlers and access 
managers.

A decode handler takes care of decoding and validating a JWT, then adds 
the decoded object to the context for future handler to use. There are two 
decode handler: one for reading the token from an authorization header 
and one to read the token from a cookie. Pick whichever you like. 
A decode handler is simply created using a helper function as follows:
```java
Handler decodeHandler = JavalinJWT.createHeaderDecodeHandler(provider);
```
**Note: it is a common mistake to think that JWT is an alternative for cookies; 
it is actually an alternative to sessions and cookies could be used for carrying 
JWTs**

And should be added as a *before* handler, whether globally or to certain paths. In this example we set it globally:
```java
app.before(decodeHandler);
```

We will then use an access manager to handle access management. An access manager 
requires the name of a JWT claim which declares the user's level, a mapping between 
users' levels and roles, and a default role for when no token is available. For the 
sake of this example, here are the available roles and their mapping:
```java
enum Roles implements Role {
    ANYONE,
    USER,
    ADMIN
}

Map<String, Role> rolesMapping = new HashMap<String, Role>() {{
    put("user", Roles.USER);
    put("admin", Roles.ADMIN);
}};
```
And the access manager is set simply like this:
```java
JWTAccessManager accessManager = new JWTAccessManager("level", rolesMapping, Roles.ANYONE);
app.accessManager(accessManager);
```
Notice that the user's level claim must match what was specified in the generator.

Now that we have the decode handler and the access manager all set up, we can go 
ahead and put them to good use.

```java
Handler generateHandler = context -> {
    MockUser mockUser = new MockUser("Mocky McMockface", "user");
    String token = provider.generateToken(mockUser);
    context.json(new JWTResponse(token));
};

Handler validateHandler = context -> {
    DecodedJWT decodedJWT = JavalinJWT.getDecodedFromContext(context);
    context.result("Hi " + decodedJWT.getClaim("name").asString());
};

app.get("/generate",  generateHandler, roles(Roles.ANYONE));
app.get("/validate", validateHandler, roles(Roles.USER, Roles.ADMIN));
app.get("/adminslounge", validateHandler, roles(Roles.ADMIN));
```

Although generateHandler remains unchanged from the previous example, validateHandler 
is now more concise and focused. You no longer need to do user authorization in the 
handlers. To highlight that, the example shows that both */validate* and */adminlounge* 
have the same handler but different access roles.
