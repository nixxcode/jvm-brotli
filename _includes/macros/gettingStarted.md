{% capture java %}
import io.javalin.Javalin;

public class HelloWorld {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        app.get("/", ctx -> ctx.result("Hello World"));
    }
}
{% endcapture %}

{% capture kotlin %}
import io.javalin.Javalin

fun main(args: Array<String>) {
    val app = Javalin.create().start(7000)
    app.get("/") { ctx -> ctx.result("Hello World") }
}
// You can wrap the main function
// in a Kotlin object
{% endcapture %}
{% include macros/docsSnippet.html java=java kotlin=kotlin %}
