---
layout: default
title: OpenAPI documentation
rightmenu: true
permalink: /plugins/openapi
---

<div id="spy-nav" class="right-menu" markdown="1">
* [Getting Started](#getting-started)
* [OpenApiOptions](#openapioptions)
* [Handler](#documenting-handler)
  * [DSL](#dsl)
  * [Annotations](#annotations)
* [CrudHandler](#documenting-crudhandler)
  * [DSL](#dsl-1)
  * [Annotations](#annotations-1)
* [Rendering](#rendering-docs)
  * [Swagger UI](#swagger-ui)
  * [ReDoc](#redoc)
</div>

<h1 class="no-margin-top">OpenAPI Plugin</h1>

This plugin allows to generate the [OpenApi specification](https://swagger.io/docs/specification/about/)
from the application source code. This can be used to [share documentation](https://swagger.io/tools/swagger-ui/)
or [generate client code](https://swagger.io/tools/swagger-codegen/).

## Getting Started

Add the dependencies:

```xml
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-models</artifactId>
    <version>2.0.8</version>
</dependency>
<dependency>
    <groupId>cc.vileda</groupId>
    <artifactId>kotlin-openapi3-dsl</artifactId>
    <version>0.20.1</version>
</dependency>
```

Register the plugin:

```java
Javalin.create(config -> {
    config.registerPlugin(new OpenApiPlugin(getOpenApiOptions()));
}).start();

private OpenApiOptions getOpenApiOptions() {
    Info applicationInfo = new Info()
        .version("1.0")
        .description("My Application");
    return new OpenApiOptions(applicationInfo).path("/swagger-docs");
}
```

The OpenApi specification is now available under the `/swagger-docs` endpoint.

## OpenApiOptions
This section contains an overview of all the available open api options.

You can either pass the info object:

```java
new OpenApiOptions(new Info().version("1.0").description("My Application"));
```

Or you can pass a lambda, which creates the initial documentation.

Here is an overview of the options:

```java
InitialConfigurationCreator initialConfigurationCreator = () -> new OpenAPI()
    .info(new Info().version("1.0").description("My Application"))
    .addServersItem(new Server().url("http://my-server.com").description("My Server"));

new OpenApiOptions(initialConfigurationCreator)
    .path("/swagger-docs") // Activate the open api endpoint
    .roles(roles(new MyRole())) // Require specific roles for the open api endpoint
    .defaultDocumentation(doc -> { doc.json("500", MyError.class); }) // Lambda that will be applied to every documentation
    .activateAnnotationScanningFor("com.my.package") // Activate annotation scanning (Required for annotation api with static java methods)
    .toJsonMapper(JacksonToJsonMapper.INSTANCE) // Custom json mapper
    .modelConverterFactory(JacksonModelConverterFactory.INSTANCE); // Custom OpenApi model converter
    .swagger(new SwaggerOptions("/swagger").title("My Swagger Documentation")) // Activate the swagger ui
    .reDoc(new ReDocOptions("/redoc").title("My ReDoc Documentation")) // Active the ReDoc UI
```

## Documenting Handler

Because of the dynamic definition of endpoints in Javalin, it is necessary to
attach some metadata to the endpoints. There are two ways to define the documentation,
either via DSL or by annotations. Both approaches can be mixed in the same application.
If both method methods are used on the same handler, the DSL documentation will be preferred.

### DSL

You can use the `document` method to create the documentation and attach it to
with the `documented` method to a `Handler`.

```java
public class MyApplication {
  public static void main(String[] args) {
      // ...
      OpenApiDocumentation createUserDocumentation = OpenApiBuilder.document()
          .body(User.class)
          .json("200", User.class);

      app.post("/users", OpenApiBuilder.documented(createUserDocumentation, ctx -> {
          // ...
      }));
  }
}
```

Here is an overview of the dsl api:

```java
OpenApiBuilder.document()
    // Update the OpenApiOperation directly
    .operation(openApiOperation -> {
        openApiOperation.description("My Operation");
        openApiOperation.operationId("myOperationId");
        openApiOperation.summary("My Summary");
        openApiOperation.deprecated(false);
        openApiOperation.addTagsItem("user");
    })

    // Parameters
    .pathParam("my-path-param", String.class, openApiParam -> {
        // You can always attach a lambda to update the OpenApi object directly
        openApiParam.description("My Path Parameter");
    })
    .queryParam("my-query-param", Integer.class)
    .header("my-custom-header")
    .cookie("my-cookie")
    .uploadedFile("my-file")
    .uploadedFiles("my-files")

    // Body
    .body(User.class)
    .bodyAsBytes("image/png")

    // Responses
    .json("200", User.class)
    .jsonArray("200", User.class) // For Arrays
    .html("200")
    .result("204") // No Content

    // Other
    .ignore(); // Hide this endpoint in the documentation
```

### Annotations

The `OpenApi` annotation can also be used to attach the OpenApi metadata to a
`Handler`.

```java
public class MyApplication {
  public static void main(String[] args) {
      // ...
      UserController userController = new UserController();
      app.post("/users", userController::createUser);
  }
}

class UserController {
    @OpenApi(
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = User.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))
        }
    )
    public void createUser(Context ctx) {
        // ...
    }
}
```

Here is an overview of the annotation api:
```java
@OpenApi(
    description = "My Operation",
    operationId = "myOperationId",
    summary = "My Summary",
    deprecated = false,
    tags = {"user"},

    // Parameters
    pathParams = {
        @OpenApiParam(name = "my-path-param", description = "My Path Parameter")
    },
    queryParams = {
        @OpenApiParam(name = "my-query-param", type = Integer.class)
    },
    headers = {
        @OpenApiParam(name = "my-custom-header")
    },
    cookies = {
        @OpenApiParam(name = "my-cookie")
    },
    fileUploads = {
        @OpenApiFileUpload(name = "my-file"),
        @OpenApiFileUpload(name = "my-files", isArray = true)
    },

    // Body
    requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = User.class)),
    requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Byte[].class, type = "image/png")),

    // Responses
    responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class, isArray = true)),
        @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html")),
        @OpenApiResponse(status = "204") // No content
    },

    // Other
    ignore = true // Hide this endpoint in the documentation
)
public void myHandler(Context ctx) {
}
```

To make the annotation api work with static java methods, a few extra steps are necessary. This is only
required for static Java methods. Static Kotlin methods or Java instance methods work by default.

1. Install the ClassGraph dependency:

    ```xml
    <dependency>
        <groupId>io.github.classgraph</groupId>
        <artifactId>classgraph</artifactId>
        <version>4.8.34</version>
    </dependency>
    ```

2. Activate annotation scanning for your package path:

    ```java
    new OpenApiOptions(applicationInfo)
            .activateAnnotationScanningFor("my.package.path")
            // ...
    ```

3. Include the the `path` and `method` parameter on the `OpenApi` annotation. These parameter are only
used for annotation scanning.

    ```java
    public class MyApplication {
      public static void main(String[] args) {
          // ...
          app.post("/users", UserController::createUser);
      }
    }

    class UserController {
        @OpenApi(
            path = "/users",
            method = HttpMethod.POST,
            // ...
        )
        public static void createUser(Context ctx) {
            // ...
        }
    }
    ```

## Documenting CrudHandler
The `CrudHandler` ([docs](/documentation#crudhandler)) is an interface with the five main CRUD operations.
This makes it a bit different from the `Handler` interface (which only has one method), but it can still be documented.

### DSL

With the dsl, you can use the `documentCrud` method:

```java
OpenApiCrudHandlerDocumentation userDocumentation = OpenApiBuilder.documentCrud()
    .getAll(OpenApiBuilder.document().jsonArray("200", User.class))
    .getOne(OpenApiBuilder.document().pathParam("id", String.class).json("200", User.class))
    .create(OpenApiBuilder.document().body(User.class).json("200", User.class))
    .update(OpenApiBuilder.document().pathParam("id", String.class).body(User.class).result("200", User.class))
    .delete(OpenApiBuilder.document().pathParam("id", String.class).result("200", User.class));

app.routes(() -> {
    ApiBuilder.crud("/users/:id", OpenApiBuilder.documented(userDocumentation, new UserCrudHandler()));
});
```

### Annotations

With the annotation api, you can just annotate the individual methods of the `CrudHandler`.

```java
public class MyApplication {
  public static void main(String[] args) {
      // ...
      app.routes(() -> {
          ApiBuilder.crud("/users/:id", new UserCrudHandler());
      });
  }
}

class UserCrudHandler implements CrudHandler {
    @OpenApi(
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class, isArray = true))
    )
    @Override
    public void getAll(@NotNull Context ctx) {
        // ...
    }

    @OpenApi(
        pathParams = @OpenApiParam(name = "id"),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))
    )
    @Override
    public void getOne(@NotNull Context ctx, @NotNull String resourceId) {
        // ...
    }

    @OpenApi(
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))
    )
    @Override
    public void create(@NotNull Context ctx) {
        // ...
    }

    @OpenApi(
        pathParams = @OpenApiParam(name = "id"),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))
    )
    @Override
    public void update(@NotNull Context ctx, @NotNull String resourceId) {
        // ...
    }

    @OpenApi(
        pathParams = @OpenApiParam(name = "id"),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))
    )
    @Override
    public void delete(@NotNull Context ctx, @NotNull String resourceId) {
        // ...
    }
}
```

## Rendering docs

The OpenAPI plugin supports both [Swagger UI](https://swagger.io/tools/swagger-ui/)
and/or [ReDoc](https://redoc.ly/) for rendering docs.

To enable this functionality you need to add the dependencies, then configure your `openApiOptions`.

### Swagger UI

Start by adding the WebJar for Swagger UI. This contains all the HTML/CSS/JavaScript you need:

```xml
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>swagger-ui</artifactId>
    <version>3.22.2</version>
</dependency>
```

You then have to enable Swagger UI on your `OpenApiOptions` object:

```java
new OpenApiOptions(applicationInfo)
    .path("/swagger-docs")
    .swagger(new SwaggerOptions("/swagger").title("My Swagger Documentation"))
```

You can have both Swagger UI and ReDoc enabled at the same time.

### ReDoc

Start by adding the WebJar for ReDoc. This contains all the HTML/CSS/JavaScript you need:

```xml
<dependency>
    <groupId>org.webjars.npm</groupId>
    <artifactId>redoc</artifactId>
    <version>2.0.0-rc.2</version>
</dependency>
```

You then have to enable ReDoc on your `OpenApiOptions` object:

```java
new OpenApiOptions(applicationInfo)
    .path("/swagger-docs")
    .reDoc(new ReDocOptions("/redoc").title("My ReDoc Documentation"))
```

You can have both ReDoc and Swagger UI enabled at the same time.

#### Acknowledgement

This plugin and its documentation was written almost
entirely by Tobias Walle ([GitHub](https://github.com/TobiasWalle) and [LinkedIn](https://www.linkedin.com/in/tobias-walle/)).

Thank you, Tobias!
