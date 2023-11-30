package com.vertx.vertx_server.model;

import java.util.UUID;

public class Item {
  private UUID id;
  private UUID owner;
  private String name;

  public Item(UUID id, UUID owner, String name) {
    this.id = id;
    this.owner = owner;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOwner() {
    return owner;
  }

  public String getName() {
    return name;
  }
}
