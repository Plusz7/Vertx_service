package com.vertx.vertx_server;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import static com.vertx.vertx_server.MainVerticle.getConfigRetrieverOptions;

public class ApplicationLauncher {

  private static final Logger LOG = LoggerFactory.getLogger(ApplicationLauncher.class);
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    ConfigRetrieverOptions options = getConfigRetrieverOptions();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);
    configRetriever.getConfig(asyncResult -> {
      if (asyncResult.succeeded()) {
        JsonObject config = asyncResult.result();
        MongoClient mongoClient = MongoClient.createShared(vertx, config.getJsonObject("mongo"));
        MainVerticle mainVerticle = new MainVerticle(vertx, mongoClient, config);

        vertx.deployVerticle(mainVerticle, result -> {
          if (result.succeeded()) {
            LOG.info("Deployment successful.");
          } else {
            LOG.error("Deployment failed: " + result.cause().getMessage());
          }
        });
      }
    });
  }
}
