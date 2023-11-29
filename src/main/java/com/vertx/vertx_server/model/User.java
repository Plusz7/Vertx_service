package com.vertx.vertx_server.model;

import java.util.UUID;

public class User {

  private UUID id;
  private String login;
  private String password;

  public User(UUID id, String login, String password) {
    this.id = id;
    this.login = login;
    this.password = password;
  }

  public UUID getId() {
    return id;
  }

  public String getLogin() {
    return login;
  }

  public String getPassword() {
    return password;
  }
}
