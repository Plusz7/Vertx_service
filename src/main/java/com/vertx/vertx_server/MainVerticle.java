package com.vertx.vertx_server;

import com.vertx.vertx_server.handler.ItemHandler;
import com.vertx.vertx_server.handler.UserHandler;
import com.vertx.vertx_server.model.Item;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;
import java.util.logging.Logger;

public class MainVerticle extends AbstractVerticle {

  private static final String ITEMS_ENDPOINT = "/items";
  private MongoClient mongoClient;
  private JWTAuth jwtAuth;

  @Override
  public void start(Promise<Void> startPromise) {
    ConfigRetrieverOptions options = getConfigRetrieverOptions();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);
    configRetriever.getConfig(ar -> {
      if (ar.succeeded()) {
        JsonObject config = ar.result();
        JsonObject jwtConfig = config.getJsonObject("jwt");
        initJWTAuth(jwtConfig);
        mongoClient = MongoClient.createShared(vertx, config.getJsonObject("mongo"));
        ItemHandler itemHandler = new ItemHandler(mongoClient);
        UserHandler userHandler = new UserHandler(mongoClient, jwtAuth);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route(ITEMS_ENDPOINT).handler(JWTAuthHandler.create(jwtAuth));

        router.post("/register").handler(userHandler::handleRegister);
        router.post("/login").handler(userHandler::handleLogin);

        router.post(ITEMS_ENDPOINT).handler(itemHandler::handleAddItem);
        router.get(ITEMS_ENDPOINT).handler(itemHandler::handleGetItems);

        createHttpServer(startPromise, config, router);
      } else {
        startPromise.fail(ar.cause());
      }
    });
  }

  private static ConfigRetrieverOptions getConfigRetrieverOptions() {
    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", "config.json"));
    return new ConfigRetrieverOptions().addStore(fileStore);
  }

  @Override
  public void stop() {
    if (mongoClient != null) {
      mongoClient.close();
    }
  }

  private void initJWTAuth(JsonObject jwtConfig) {
    System.out.println("JWT Config: " + jwtConfig.encodePrettily());
    jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer(jwtConfig.getString("secret"))
      ));
  }

  private void createHttpServer(Promise<Void> startPromise, JsonObject config, Router router) {
    HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
    Integer httpPort = config.getInteger("http.port");
    vertx.createHttpServer(options).requestHandler(router).listen(httpPort, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port " + httpPort);
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
