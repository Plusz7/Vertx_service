package com.vertx.vertx_server;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class MainVerticleResponsesTest {

  private Vertx vertx;

  @BeforeEach
  void setup(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle(), testContext.succeedingThenComplete());
  }

  @AfterEach
  void teardown() {
    vertx.close();
  }

  @RepeatedTest(3)
  void httpServerCheckUnauthorizedResponse(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    client.request(HttpMethod.GET, 3000, "localhost", "/items")
      .compose(req -> req.send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      }))));
  }

  @Test
  void httpServerCheckRegisterSuccessResponse(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    JsonObject requestBody = new JsonObject()
      .put("login", "testUser")
      .put("password", "testPass");

    client.request(HttpMethod.POST, 3000, "localhost", "/register")
      .compose(req -> req.putHeader("content-type", "application/json")
        .send(Buffer.buffer(requestBody.encode())))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(201, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  void httpServerCheckRegisterFailResponse(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    JsonObject userRequestBody = new JsonObject()
      .put("username", null)
      .put("password", "testPass");

    client.request(HttpMethod.POST, 3000, "localhost", "/register")
      .compose(req -> req.putHeader("content-type", "application/json")
        .send(Buffer.buffer(userRequestBody.encode())))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        testContext.completeNow();
      })));
  }


}
