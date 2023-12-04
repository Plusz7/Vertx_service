package com.vertx.vertx_server;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.vertx.vertx_server.MainVerticle.getConfigRetrieverOptions;
import static org.mockito.Mockito.mock;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    MongoClient mockMongoClient = mock(MongoClient.class);
    ConfigRetrieverOptions options = getConfigRetrieverOptions();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);
    configRetriever.getConfig(asyncResult -> {
      if (asyncResult.succeeded()) {
        vertx.deployVerticle(new MainVerticle(vertx, mockMongoClient, asyncResult.result()), testContext.succeeding(id -> testContext.completeNow()));
      }
    });
  }

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }
}
