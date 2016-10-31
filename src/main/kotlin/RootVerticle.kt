package com.veeroute.fb

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions

class RootVerticle : AbstractVerticle() {
    override fun start() {
        val options = DeploymentOptions().setConfig(config())

        vertx.deployVerticle("com.veeroute.fb.WebVerticle", options);
        vertx.deployVerticle("com.veeroute.fb.PageVerticle", options);
    }
}
