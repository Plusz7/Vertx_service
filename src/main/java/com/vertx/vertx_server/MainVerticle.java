package com.vertx.vertx_server;

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

  private MongoClient mongoClient;
  private JWTAuth jwtAuth;

  @Override
  public void start(Promise<Void> startPromise) {
    ConfigRetrieverOptions options = getConfigRetrieverOptions();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);
    configRetriever.getConfig(ar -> {
      if (ar.succeeded()) {
        JsonObject config = ar.result();
        System.out.println("_________________");
        System.out.println(config.encodePrettily());
        System.out.println("___________________");
        JsonObject jwtConfig = config.getJsonObject("jwt");
        System.out.println("*********************");
        System.out.println(jwtConfig.encodePrettily());
        System.out.println("*******************");
        initJWTAuth(jwtConfig);
        mongoClient = MongoClient.createShared(vertx, config.getJsonObject("mongo"));
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuth);
        router.route("/items").handler(JWTAuthHandler.create(jwtAuth));

        router.post("/register").handler(this::handleRegister);
        router.post("/login").handler(this::handleLogin);

        router.post("/items").handler(this::handleAddItem);
        router.get("/items").handler(this::handleGetItems);

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

  private void handleRegister(RoutingContext context) {
    JsonObject body = context.getBodyAsJson();
    String login = body.getString("login");
    String password = body.getString("password");

    String hashedPassword = hashPassword(password);

    JsonObject newUser = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("login", login)
      .put("password", hashedPassword);

    mongoClient.save("users", newUser, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end("Registering successfull.");
      } else {
        context.response().setStatusCode(500).end("User registration failed");
      }
    });
  }

  private void handleLogin(RoutingContext context) {
    JsonObject body = context.getBodyAsJson();
    String login = body.getString("login");
    String password = body.getString("password");

    JsonObject query = new JsonObject().put("login", login);
    mongoClient.findOne("users", query, null, lookup -> {
      if (lookup.succeeded()) {
        JsonObject user = lookup.result();
        if (user != null && BCrypt.checkpw(password, user.getString("password"))) {
          String token = jwtAuth.generateToken(
            new JsonObject().put("sub", user.getString("login")),
            new JWTOptions().setExpiresInSeconds(60 * 60)
          );
          context.response()
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("token", token).encode());
        } else {
          context.response().setStatusCode(401).end("Invalid credentials");
        }
      } else {
        context.response().setStatusCode(500).end("Authentication failed");
      }
    });
  }

  private void handleAddItem(RoutingContext context) {
    JsonObject userPrincipal = context.user().principal();
    String ownerId = userPrincipal.getString("sub");

    JsonObject body = context.getBodyAsJson();
    if (body == null) {
      context.response().setStatusCode(400).end("Invalid JSON");
      return;
    }

    Item item = new Item(UUID.randomUUID(), UUID.fromString(body.getString(ownerId)), body.getString("name"));
    JsonObject itemJson = JsonObject.mapFrom(item);
    mongoClient.save("items", itemJson, res -> {
      if (res.succeeded()) {
        context.response()
          .setStatusCode(201)
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("id", res.result()).encode());
      } else {
        context.response().setStatusCode(500).end("Failed to save item");
      }
    });
  }

  private void handleGetItems(RoutingContext context) {
    mongoClient.find("items", new JsonObject(), res -> {
      if (res.succeeded()) {
        context.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json")
          .end(res.result().toString());
      } else {
        context.response().setStatusCode(500).end("Failed to retrieve items");
      }
    });
  }

  private String hashPassword(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }

}
