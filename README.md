# Spring Cloud Gateway Demo

This is a small API Gateway demo using Spring Cloud Gateway. It was done
for [my talk at CodeMash 2024](https://codemash.org/session-details/?id=536315), [slides](https://github.com/dmikusa/scg-codemash-demo/blob/main/API%20Slides%20-%20CodeMash.pdf).

There are some client templates set up using [Bruno](https://www.usebruno.com/), you can open them or just look at the
text and make your own requests. Any tool like `curl` or Postman will work just fine.

## Facade Pattern

If you want to run and check out the Facade Pattern, run `./gradlew bootRun`.

This will expose two endpoints `/give-me-dogs` and `/give-me-cats` which will return a list of dogs and cats
respectively. They point to the services https://dog.ceo and https://cataas.com/.

This demo shows the facade pattern and how you put a unique interface in front of the two APIs without modifying the
APIs. See the `application-facade.yml` file for specifics on how SCG was configured.

Highlights:

- we change the paths around
- we add some request params
- we rewrite one of the endpoint's response
- we reformat one of the request URIs

## Spy Pattern

This one is a little more difficult to demo, but it's been enabled for all of the demos here. It allows us to spy on
requests and capture some metrics about them. You can then use the Spring Boot Actuator Metrics endpoint to examine them
or you can hook things up to your metrics system, like Prometheus.

In addition, by adding one system property to the application we can enable access logging which is a great thing to
have in production. This is done by adding the system flag `-Dreactor.netty.http.server.accessLogEnabled=true`. In this
demo, it's added in `build.gradle`, but it could be added anywhere you normally add your JVM flags.

```
bootRun {
    // enables Netty access logging
    jvmArgs(['-Dreactor.netty.http.server.accessLogEnabled=true'])
}
```

With it enabled, you'll get lines like:

```
2024-01-10T15:48:19.020-05:00  INFO 32005 --- [ctor-http-nio-2] reactor.netty.http.server.AccessLog      : 0:0:0:0:0:0:0:1 - - [10/Jan/2024:15:48:18 -0500] "GET /give-me-cats HTTP/1.1" 200 161 1017
```

These could be fed into your log aggregation system for further analysis.

## Transformer

In the Facade demo, we saw how we could rewrite the response from one of the APIs. This is being done
in `RewriteDogJsonGatewayFilterFactory`. The code uses Jackson to parse the JSON response from the upstream API and
then rewrites it to a new JSON response. This is a very simple example, but it shows how you can transform the response.
