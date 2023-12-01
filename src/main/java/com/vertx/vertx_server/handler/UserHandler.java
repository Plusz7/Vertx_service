package com.vertx.vertx_server.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class UserHandler {

  private static final String ID = "id";
  private static final String LOGIN = "login";
  private static final String PASSWORD = "password";
  private static final String MONGODB_USERS_COLLECTION = "users";

  private final MongoClient mongoClient;
  private final JWTAuth jwtAuth;

  public UserHandler(
    MongoClient mongoClient,
    JWTAuth jwtAuth
  ) {
    this.mongoClient = mongoClient;
    this.jwtAuth = jwtAuth;
  }

  public void handleRegister(RoutingContext context) {
    JsonObject body = context.getBodyAsJson();
    String login = body.getString(LOGIN);
    String password = body.getString(PASSWORD);

    String hashedPassword = hashPassword(password);

    JsonObject newUser = new JsonObject()
      .put(ID, UUID.randomUUID().toString())
      .put(LOGIN, login)
      .put(PASSWORD, hashedPassword);

    mongoClient.save(MONGODB_USERS_COLLECTION, newUser, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(204).end("Registering successfull.");
      } else {
        context.response().setStatusCode(500).end("User registration failed");
      }
    });
  }

  public void handleLogin(RoutingContext context) {
    JsonObject body = context.getBodyAsJson();
    String login = body.getString(LOGIN);
    String password = body.getString(PASSWORD);

    JsonObject query = new JsonObject().put(LOGIN, login);
    mongoClient.findOne(MONGODB_USERS_COLLECTION, query, null, lookup -> {
      if (lookup.succeeded()) {
        JsonObject user = lookup.result();
        if (user != null && BCrypt.checkpw(password, user.getString(PASSWORD))) {
          String token = jwtAuth.generateToken(
            new JsonObject().put("ownerId", user.getString(ID)),
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

  private String hashPassword(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }
}
