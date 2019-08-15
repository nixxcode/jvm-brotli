---
layout: default
title: Tutorials
permalink: /tutorials/
---

{% include newJavalinBanner.html %}

<h1 class="no-margin-top">Tutorials</h1>

{% assign tutorials = (site.posts | where: "layout" , "tutorial") | sort: 'date' | reverse %}
{% assign jTuts = (tutorials | where: "language" , "java") %}
{% assign kTuts = (tutorials | where: "language" , "kotlin") %}

<div class="posts-header" markdown="1">
We recommend starting with either the [Maven setup](maven-setup) or [Gradle setup](gradle-setup) tutorial,
then going through the [Kotlin CRUD REST API](/tutorials/simple-kotlin-example) tutorial.
Most of the tutorials can be followed in either language.
</div>

<div class="posts-overview">
    <ul class="post-list half">
        <h2>Kotlin tutorials</h2>
        {% for tutorial in kTuts %}
            <li class="post-summary">
                <a href="{{ tutorial.url }}">
                  <h2>{{ tutorial.summarytitle }}</h2>
                  <p>{{ tutorial.summary }}</p>
              </a>
            </li>
        {% endfor %}
    </ul>
     <ul class="post-list half">
            <h2>Java tutorials</h2>
            {% for tutorial in jTuts %}
            <li class="post-summary">
                <a href="{{ tutorial.url }}">
                    <h2>{{ tutorial.summarytitle }}</h2>
                    <p>{{ tutorial.summary }}</p>
                </a>
            </li>
            {% endfor %}
        </ul>
</div>
<div class="posts-footer" markdown="1">
The tutorials here are written by Javalin users and posted with their permission.
If you have have a tutorial you want to submit, please create a pull request on [GitHub](https://github.com/javalin/javalin.github.io).
</div>
