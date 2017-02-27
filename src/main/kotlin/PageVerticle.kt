package com.veeroute.fb

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.lang.Buffer

class PageVerticle : AbstractVerticle() {
    var client : HttpClient? = null;

    override fun start(fut: Future<Void>) {
        val eb = vertx.eventBus();

        val pageId = config().getString("PAGE_ID")
        println("PageID=$pageId")

        val consumer = eb.consumer<JsonObject>("page.$pageId");
        consumer.handler({
            message ->
            val pageEntry = message.body();
            var pageID = pageEntry.map["id"];
            var timeOfEvent = pageEntry.map["time"];
            println("got data with pageID = $pageID, timeOfEvent = $timeOfEvent")

            // Iterate over each messaging event
            pageEntry.getJsonArray("messaging").forEach({ event ->
                val messagingEvent = event as JsonObject;
                if (messagingEvent.map["optin"] != null) {
                    println("receivedAuthentication $messagingEvent")
                } else if (messagingEvent.map["message"] != null) {
                    receivedMessage(event);
                } else if (messagingEvent.map["delivery"] != null) {
                    println("receivedDeliveryConfirmation $messagingEvent")
                } else if (messagingEvent.map["postback"] != null) {
                    receivedPostback(event);
                } else {
                    println("Webhook received unknown messagingEvent: $messagingEvent")
                }
            });
        })

        client = vertx.createHttpClient(HttpClientOptions().setSsl(true).setLogActivity(true));

        fut.complete();
    }

    fun callSendAPI(messageData: JsonObject) {
        val PAGE_ACCESS_TOKEN = config().getString("PAGE_ACCESS_TOKEN")
        val request = client!!.post(443, "graph.facebook.com", "/v2.6/me/messages?access_token=$PAGE_ACCESS_TOKEN",
                { response ->

                    println("message sent, got response with a code ${response.statusCode()}")
                })
        val buf = Buffer() {
            appendString(Json.encode(messageData))
        };
        println("printing ${buf.toString("utf-8")}")
        request.putHeader("content-type", "application/json")
        request.putHeader("content-length", buf.length().toString())
        request.write(buf);
        request.end()
    }

    fun sendTextMessage(recipientId: String, messageText: String) {
        callSendAPI(JsonObject(mapOf("recipient" to mapOf("id" to recipientId),
                "message" to mapOf("text" to messageText))));
    }

    fun sendGenericMessage(recipientId: String) {
        callSendAPI(JsonObject(mapOf(
                "recipient" to mapOf("id" to recipientId),
                "message" to oculusRiftAttachment
        )));
    }

    fun receivedMessage(event: JsonObject) {
        println("receivedMessage $event")
        val txt = event.getJsonObject("message")?.getString("text")
        val senderID = event.getJsonObject("sender")?.getString("id")
        if (txt != null && senderID != null) {
            if (txt == "generic") {
                sendGenericMessage(senderID);
            } else {
                sendTextMessage(senderID, txt);
            }
        }
    }

    fun receivedPostback(event: JsonObject) {
        val recipientID = event.getJsonObject("recipient")?.getString("id")
        val senderID = event.getJsonObject("sender")?.getString("id")
        var timeOfPostback = event.getJsonObject("timestamp");
        var payload = event.getJsonObject("postback")?.getString("payload");
        if (recipientID != null &&
                senderID != null &&
                timeOfPostback != null &&
                payload != null) {
            println("Received postback for user $senderID and page $recipientID with payload '$payload' at $timeOfPostback");
            sendTextMessage(senderID, "Postback called");
        }
    }

    companion object {
        val oculusRiftAttachment = mapOf(
                "attachment" to mapOf(
                        "type" to "template",
                        "payload" to mapOf(
                                "template_type" to "generic",
                                "elements" to arrayOf(
                                        mapOf(
                                                "title" to "rift",
                                                "subtitle" to "Next-generation virtual reality",
                                                "item_url" to "https://www.oculus.com/en-us/rift/",
                                                "image_url" to "http://messengerdemo.parseapp.com/img/rift.png",
                                                "buttons" to arrayOf(
                                                        mapOf(
                                                                "type" to "web_url",
                                                                "url" to "https://www.oculus.com/en-us/rift/",
                                                                "title" to "Open Web URL"
                                                        ),
                                                        mapOf(
                                                                "type" to "postback",
                                                                "title" to "Call Postback",
                                                                "payload" to "Payload for first bubble"
                                                        )
                                                )
                                        ),
                                        mapOf(
                                                "title" to "touch",
                                                "subtitle" to "Your Hands, Now in VR",
                                                "item_url" to "https://www.oculus.com/en-us/rift/",
                                                "image_url" to "http://messengerdemo.parseapp.com/img/touch.png",
                                                "buttons" to arrayOf(
                                                        mapOf("type" to "web_url",
                                                                "url" to "https://www.oculus.com/en-us/touch/",
                                                                "title" to "Open Web URL"),
                                                        mapOf("type" to "postback",
                                                                "title" to "Call Postback",
                                                                "payload" to "Payload for second bubble"
                                                        )
                                                ))
                                )
                        )
                )
        )
    }
}
