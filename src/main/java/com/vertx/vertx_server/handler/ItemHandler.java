package com.vertx.vertx_server.handler;

import com.vertx.vertx_server.model.Item;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class ItemHandler {
  private MongoClient mongoClient;

  public ItemHandler(
    MongoClient mongoClient
  ) {
    this.mongoClient = mongoClient;
  }

  public void handleAddItem(RoutingContext context) {
    JsonObject userPrincipal = context.user().principal();
    System.out.println(userPrincipal.encodePrettily());
    String ownerId = userPrincipal.getString("ownerId");
    System.out.println(ownerId);

    JsonObject body = context.getBodyAsJson();
    if (body == null) {
      context.response().setStatusCode(400).end("Invalid JSON");
      return;
    }

    String itemName = body.getString("name");
    System.out.println(itemName);
    if (itemName == null) {
      context.response().setStatusCode(400).end("Item name is missing");
      return;
    }

    Item item = new Item(UUID.randomUUID(), UUID.fromString(ownerId), itemName);
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

  public void handleGetItems(RoutingContext context) {
    JsonObject userPrincipal = context.user().principal();
    String ownerId = userPrincipal.getString("ownerId");
    JsonObject query = new JsonObject().put("owner", ownerId);
    mongoClient.find("items", query, res -> {
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
}
