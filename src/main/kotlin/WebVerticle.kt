package com.veeroute.fb

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

class WebVerticle : AbstractVerticle() {
    val router = Router.router(vertx)

    init {
        router.route().handler(BodyHandler.create())
        router.route("/webhook").handler({ routingContext ->
            val req = routingContext.request()
            val res = routingContext.response()
            val query = req.params()

            val eventBus = vertx.eventBus()

            if (query["hub.mode"] == "subscribe") {
                if (query["hub.verify_token"] == config().getString("VALIDATION_TOKEN")) {
                    println("Validating webhook");
                    res.end(query["hub.challenge"]);
                } else {
                    println("Failed validation. Make sure the validation tokens match.");
                    res.setStatusCode(403).end();
                }
            } else {
                val data = routingContext.bodyAsJson;
                if (data != null) {
                    if (data.map["object"] == "page") {
                        // Iterate over each entry
                        // There may be multiple if batched
                        println("Got some stuff $data")
                        data.getJsonArray("entry").forEach({ entry ->
                            val pageEntry = entry as JsonObject;
                            var pageID = pageEntry.map["id"];
                            var timeOfEvent = pageEntry.map["time"];

                            println("HTTP webhook got data with pageID = $pageID, timeOfEvent = $timeOfEvent")
                            if (pageID != null) {
                                eventBus.publish("page.$pageID", pageEntry)
                            }
                        });
                    }
                }
                res.end()
            }
        })
    }

    override fun start(fut: Future<Void>) {
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port", 7000), { result ->
                    if (result.succeeded()) {
                        fut.complete();
                    } else {
                        fut.fail(result.cause());
                    }
                });
    }
}
