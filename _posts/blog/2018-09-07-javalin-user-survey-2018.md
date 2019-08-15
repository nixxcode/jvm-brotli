---
layout: default
category: blog
date: 2018-09-07
title: Javalin user survey responses
summary: We ran a user-survey on https://javalin.io for a couple of months
permalink: blog/javalin-user-survey-2018
---

## Javalin user survey, Summer 2018
We polled a total of 100 users on [javalin.io](/). To filter out non-users
the survey link was only shown to users who had performed 5 page loads.
That wasn't enough though, as it turned out a third of respondents were not actually using Javalin yet.
These responses have been filtered out.

## General

### Q: What role best describes you?

<div class="bar-chart">
    <div style="width:68%" data-value="68%">Software engineer / developer</div>
    <div style="width:16%" data-value="16%">Tech lead / manager</div>
    <div style="width:7%" data-value="7%">Hobbyist programmer</div>
    <div style="width:10%" data-value="10%">Student</div>
</div>

No real suprises here. Most of our users are Software Engineers / Developers.
The number of tech leads and managers is perhaps a bit higher than expected, but not by a lot.

### Q: What are you using Javalin for?
<div class="bar-chart">
    <div style="width:86%" data-value="86%">REST APIs</div>
    <div style="width:34%" data-value="34%">WebSockets</div>
    <div style="width:39%" data-value="39%">Websites</div>
    <div style="width:1%" data-value="1%">Maven Server</div>
</div>

As expected, most people use Javalin to create REST APIs.
A surprisingly high number of people also use Javalin for creating websites,
and there was one person using it for a local Maven Server (!).

### Q: What language are you using Javalin with?
<div class="bar-chart">
    <div style="width:57%" data-value="57%">Java</div>
    <div style="width:61%" data-value="61%">Kotlin</div>
    <div style="width:2%" data-value="2%">Other</div>
</div>

When Javalin was created, the goal was to achieve a great developer experience in both Java and Kotlin.
This seems to have been a good goal, since the split is about equal (it was possible to select both languages
for this answer).

### Q: Are you using Javalin in production?
<div class="bar-chart">
    <div style="width:30%" data-value="30%">Yes</div>
    <div style="width:45%" data-value="45%">Soon</div>
    <div style="width:25%" data-value="25%">No</div>
</div>

Most users are either running in production right now, or will be soon.
Javalin appears to be a popular choice for startups and new projects, so this makes sense.

## Production usage
Only the users who replied yes to `Are you using Javalin in production` were able to answer the next three questions.

### Q: How many users does your application have?
<div class="bar-chart">
    <div style="width:63%" data-value="63%">Less than 1k per day</div>
    <div style="width:16%" data-value="16%">1k - 10k per day</div>
    <div style="width:5%" data-value="5%">10k - 100k per day</div>
    <div style="width:10%" data-value="10%">100k - 1m per day</div>
    <div style="width:0%;background:transparent;" data-value="0%">More than 1m per day</div>
</div>

Most Javalin apps appear to be fairly low traffic.
Even one million requests per day is only 12 requests per second, which is a tiny fraction of what Javalin can handle.
Javalin adds a very small overhead to Jetty (less than 5%), and can easily serve thousands of requests per second.

### Q: Approximately how big is your application?
<div class="bar-chart">
    <div style="width:37%" data-value="37%">Less than 1k lines of code</div>
    <div style="width:63%" data-value="63%">Between 1k and 10k lines of code</div>
    <div style="width:0%;background:transparent;" data-value="0%">More than 10k lines of code</div>
</div>

No surprises here. Small codebases are great!

### Q: How many people are working on your application?
<div class="bar-chart">
    <div style="width:79%" data-value="79%">1 - 2 people</div>
    <div style="width:21%" data-value="21%">2 - 5 people</div>
    <div style="width:0%;background:transparent;" data-value="0%">More than 5 people</div>
</div>

## Conclusion

Hopefully this survey provides some insight.
We'll run it again next year with the same questions and see if anything changes. Thanks for reading!

<style>
    .bar-chart {
        margin-top: 20px;
        border: 1px solid #ddd;
        border-radius: 5px;
        background: #fff;
        padding: 10px 60px 10px 10px;
        font-family: arial, sans-serif;
        position: relative;
    }

    .bar-chart > div {
        height: 28px;
        line-height: 28px;
        padding: 0 10px;
        background: #c7e6f5;
        font-size: 15px;
        border-radius: 3px;
        white-space: nowrap;
    }

    .bar-chart > div + div {
        margin-top: 10px;
    }

    .bar-chart > div::before {
        content: " ";
        position: absolute;
        width: calc(100% - 70px); /* padding x 60 x 10 */
        background: rgba(0, 0, 0, 0.08);
        height: 28px;
        border-radius: 3px;
        left: 10px;
    }

    .bar-chart > div::after {
        content: attr(data-value);
        position: absolute;
        right: 15px;
        color: #008cbb;
    }
</style>
