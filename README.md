# vertx-facebook-messenger
Seed project for [facebook messenger](https://developers.facebook.com/docs/messenger-platform/product-overview) bots. Vertx, Kotlin.

It will be compliant with [messenger platform samples](https://github.com/fbsamples/messenger-platform-samples).

## Facebook configuration

Follow [messenger platform](https://developers.facebook.com/docs/messenger-platform/product-overview) guidelines. Use [ngrok](https://ngrok.com/) to obtain direct IP for testing.

Copy "conf/bot-default.json" into "conf/bot-dev.json". Specify your facebook page data there.

## Build

### Run in gradle

Use gradle or gradlew, it depends on your system.

```bash
gradle runExample
```

### Run from Intellij Idea

Specify main class "io.vertx.core.Launcher", program arguments "run com.veeroute.fb.RootVerticle -conf conf/bot-dev.json"

## Structure

For now its just three verticles: root, web and page.
