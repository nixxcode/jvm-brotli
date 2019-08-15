
<div class="multitab-code" data-tab="1">
<ul>
    <li data-tab="1">Maven</li>
    <li data-tab="2">Gradle</li>
    <li data-tab="3">SBT</li>
    <li data-tab="4">Grape</li>
    <li data-tab="5">Leiningen</li>
    <li data-tab="6">Buildr</li>
    <li data-tab="7">Ivy</li>
</ul>

<div data-tab="1" markdown="1">
~~~markup
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>{{site.javalinversion}}</version>
</dependency>
~~~
[Not familiar with Maven? Click here for more detailed instructions.](/tutorials/maven-setup)
</div>

<div data-tab="2" markdown="1">
~~~java
compile 'io.javalin:javalin:{{site.javalinversion}}'
~~~
[Not familiar with Gradle? Click here for more detailed instructions.](/tutorials/gradle-setup)
</div>

<div data-tab="3" markdown="1">
~~~java
libraryDependencies += "io.javalin" % "javalin" % "{{site.javalinversion}}"
~~~
</div>

<div data-tab="4" markdown="1">
~~~java
@Grab(group='io.javalin', module='javalin', version='{{site.javalinversion}}') 
~~~
</div>

<div data-tab="5" markdown="1">
~~~java
[io.javalin/javalin "{{site.javalinversion}}"]
~~~
</div>

<div data-tab="6" markdown="1">
~~~java
'io.javalin:javalin:jar:{{site.javalinversion}}'
~~~
</div>

<div data-tab="7" markdown="1">
~~~markup
<dependency org="io.javalin" name="javalin" rev="{{site.javalinversion}}" />
~~~
</div>

</div>


