package com.vertx.vertx_server.handler;

import com.vertx.vertx_server.model.Item;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class ItemHandler {

  private static final String MONGODB_ITEMS_COLLECTION = "items";
  private static final String OWNER_ID = "ownerId";
  private final MongoClient mongoClient;

  public ItemHandler(
    MongoClient mongoClient
  ) {
    this.mongoClient = mongoClient;
  }

  public void handleAddItem(RoutingContext context) {
    JsonObject userPrincipal = context.user().principal();
    String ownerId = userPrincipal.getString(OWNER_ID);

    JsonObject body = context.getBodyAsJson();
    if (body == null) {
      context.response().setStatusCode(400).end("JSON is null.");
      return;
    }

    String itemName = body.getString("name");
    if (itemName == null || itemName.isEmpty()) {
      context.response().setStatusCode(400).end("Item name is missing");
      return;
    }

    Item item = new Item(
      UUID.randomUUID(),
      UUID.fromString(ownerId),
      itemName
    );

    mongoClient.save(MONGODB_ITEMS_COLLECTION, JsonObject.mapFrom(item), res -> {
      if (res.succeeded()) {
        context.response()
          .setStatusCode(201)
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("id", res.result()).encode());
      } else {
        context.response().setStatusCode(500).end("Failed to save item" + "\n" + res.cause().getMessage());
      }
    });
  }

  public void handleGetItems(RoutingContext context) {
    JsonObject userPrincipal = context.user().principal();
    String ownerId = userPrincipal.getString(OWNER_ID);
    JsonObject query = new JsonObject().put("owner", ownerId);
    mongoClient.find(MONGODB_ITEMS_COLLECTION, query, res -> {
      if (res.succeeded()) {
        context.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json")
          .end(res.result().toString());
      } else {
        context.response().setStatusCode(500).end("Failed to retrieve items" + "\n" + res.cause().getMessage());
      }
    });
  }
}
