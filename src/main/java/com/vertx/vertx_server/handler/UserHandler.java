package com.vertx.vertx_server.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;
import java.util.regex.Pattern;

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

    if(login == null || password == null) {
      context.response().setStatusCode(400).end("Must provide a login and/or password.");
      return;
    }

    if(login.isEmpty() || password.isEmpty()) {
      context.response().setStatusCode(400).end("Must provide a login and/or password.");
      return;
    }

    if(!isEmail(login)) {
      context.response().setStatusCode(400).end("Must provide login as email.");
      return;
    }


    String hashedPassword = hashPassword(password);

    JsonObject newUser = new JsonObject()
      .put(ID, UUID.randomUUID().toString())
      .put(LOGIN, login)
      .put(PASSWORD, hashedPassword);

    mongoClient.save(MONGODB_USERS_COLLECTION, newUser, res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(201).end("Registering successfull.");
      } else {
        context.response().setStatusCode(500).end("User registration failed" + "\n" + res.cause().getMessage());
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
            new JWTOptions().setExpiresInSeconds(60 * 5)
          );
          context.response()
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("token", token).encode());
        } else {
          context.response().setStatusCode(401).end("Invalid credentials");
        }
      } else {
        context.response().setStatusCode(500).end(lookup.cause().getMessage());
      }
    });
  }

  private String hashPassword(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }

  private boolean isEmail(String email) {
    Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
    return pattern.matcher(email).matches();
  }
}
