---
layout: default
title: News - Jvm-Brotli
navlogo: true
permalink: /news
---
<style>{% include landing.css %}</style>

# News
<div class="post">
	<h3>Version 0.2.0 released:</h3>
	<p class="date">08 August 2019</p>
</div>
BrotliLoader.loadBrotli() has been deprecated and replaced by a new method:
```java
	BrotliLoader.isBrotliAvailable();
```
This static method call attempts to load the native Brotli library into the current JVM runtime when invoked for the first time. If loading succeeds, it silently returns true. If loading fails, it prints an exception to console and returns false. Subsequent calls only return true/false, depending if the original load attempt was successful or not. No further load attempts are made unless the application is restarted.

Additionally: 
* Users should now get a clear error message if their platform or architecture are currently unsupported.
* Example project has been added. See the [Examples](https://jvmbrotli.com/examples) page for details and a link.

<div class="post">
	<h3>Version 0.1.1 released:</h3>
	<p class="date">03 August 2019</p>
</div>
Jvm-Brotli is officially live!