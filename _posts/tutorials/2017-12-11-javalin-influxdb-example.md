---
layout: tutorial
title: "Collecting real time analytics with influxDB"
author: <a href="https://www.linkedin.com/in/ricardo-baumann-594b6b31/" target="_blank">Ricardo Baumann</a>
date: 2017-12-11
permalink: /tutorials/influxdb
github: https://github.com/ricardobaumann/real-time-statistics
summarytitle: Real time analytics with influxDB 
summary: Learn how to setup Javalin with docker and influxDB
language: kotlin
---

## What is InfluxDB?
<blockquote>
    <p>
        InfluxDB is a database for time series data, like metrics, analytics, IoT monitoring and etc. Its fast, available and scalable. 
        For more details, visit 
        &mdash; <a href="https://www.influxdata.com/">https://www.influxdata.com/</a>
    </p>
</blockquote>

## Initial setup
Before we get started, there are a few things we need to do:

* Set up Docker [(Install Docker)](https://docs.docker.com/engine/installation/)
* Install [Gradle](https://docs.gradle.org/current/userguide/installation.html)

## Architecture
The is a javalin microservice application, with 2 endpoints:
- `POST /upload`: Receive events and insert them on database. If the event is old, discard it.
- `GET /statistics`: Return a summary of the events (count, sum, min and max)

## Implementation
The application is made of 2 simple kotlin files:
- `realtimestatistics.Main`: Creates the endpoints and application settings

```kotlin
data class Statistic(val count: Int = 0, val timestamp: Long = Date().time)

data class Total(val count: Double, val sum: Double, val min: Double, val max: Double)

val influxHost = System.getenv().getOrDefault("influx.host", "influxdb")!!

val influxDB: InfluxDB by lazy { InfluxDBFactory.connect("http://$influxHost:8086", "root", "root") }

fun main(args: Array<String>) {
    val app = Javalin.create().start(7000)
    val statisticService = StatisticsService(influxDB)
    val controller = Controller(statisticService)

    app.routes {
        get("/statistics", { ctx ->
            controller.get(ctx)
        })
        post("/upload", { ctx ->
            controller.post(ctx)
        })
    }

}

class Controller(private val statisticService: StatisticsService) {
    private val asStatusCode = fun StatisticResult.(): Int {
        return if (this == StatisticResult.OK) {
            201
        } else {
            204
        }
    }

    fun post(ctx: Context) {
        val statistic = ctx.bodyAsClass(Statistic::class.java)
        val result = statisticService.create(statistic)
        ctx.status(result.asStatusCode())
    }

    fun get(ctx: Context) {
        ctx.json(statisticService.aggregated())
    }
}
```
- `realtimestatistics.StatisticsService`: Contains the business logic to create and retrieve the metrics, plus the database initialization

```kotlin
private val timeFrameInMillis = 60000

private val aggregateQuery = """
    SELECT  count(s_count) as count,
            sum(s_count) as sum,
            min(s_count) as min,
            max(s_count) as max
    FROM uploads
    where time > now() - 60s
    """

init {
    influxDB.createDatabase(dbName)
}

fun create(statistic: Statistic): StatisticResult {
    val now = Date().time
    if ((statistic.timestamp + timeFrameInMillis) >= now) {
        influxDB.write(dbName, "", Point.measurement("uploads")
                .time(statistic.timestamp, TimeUnit.MILLISECONDS)
                .addField("s_count", statistic.count)
                .addField("s_timestamp", statistic.timestamp)
                .build())
        return StatisticResult.OK
    }
    return StatisticResult.OLD
}

fun aggregated(): Total {
    val query = Query(
            aggregateQuery,
            dbName
    )
    val results = influxDB.query(query)
            .results
    if (results.first().series == null) {
        return Total(0.0, 0.0, 0.0, 0.0)
    }
    return results.first().series.first().values
            .map { mutableList ->
                Total(mutableList[1].toString().toDouble(),
                        mutableList[2].toString().toDouble(),
                        mutableList[3].toString().toDouble(),
                        mutableList[4].toString().toDouble()
                )
            }[0]
}
```
For the full source code, check [https://github.com/ricardobaumann/real-time-statistics](https://github.com/ricardobaumann/real-time-statistics)
## Running locally
For local running, I am using docker compose. So, on the root folder, run
`docker-compose up`
and checkout the endpoints above mentioned at http://localhost:7000/

## Usage
With the service running, try POSTing to `/upload` with
`{
 	"count" : 40
}`

And then, GET the summary from `/statistics`. 

Voila, boys and girls. Please let me know your insights about it. Thanks.  
