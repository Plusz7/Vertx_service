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
  private static final String LOCALHOST = "localhost";
  private static final String REGISTER_ENDPOINT = "/register";
  private static final String CONTENT_TYPE = "content-type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String LOGIN = "login";
  private static final String PASSWORD = "password";

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
    client.request(HttpMethod.GET, 3000, LOCALHOST, "/items")
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
      .put(LOGIN, "testUser@mail.com")
      .put(PASSWORD, "testPass");

    client.request(HttpMethod.POST, 3000, LOCALHOST, REGISTER_ENDPOINT)
      .compose(req -> req.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .send(Buffer.buffer(requestBody.encode())))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(201, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  void httpServerCheckRegisterNotEmailResponse(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    JsonObject requestBody = new JsonObject()
      .put(LOGIN, "testUser")
      .put(PASSWORD, "testPass");

    client.request(HttpMethod.POST, 3000, LOCALHOST, REGISTER_ENDPOINT)
      .compose(req -> req.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .send(Buffer.buffer(requestBody.encode())))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  void httpServerCheckRegisterEmptyLoginResponse(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    JsonObject requestBody = new JsonObject()
      .put(LOGIN, "")
      .put(PASSWORD, "testPass");

    client.request(HttpMethod.POST, 3000, LOCALHOST, REGISTER_ENDPOINT)
      .compose(req -> req.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .send(Buffer.buffer(requestBody.encode())))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  void httpServerCheckRegisterFailResponse(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    JsonObject userRequestBody = new JsonObject()
      .put(LOGIN, null)
      .put(PASSWORD, "testPass");

    client.request(HttpMethod.POST, 3000, LOCALHOST, REGISTER_ENDPOINT)
      .compose(req -> req.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .send(Buffer.buffer(userRequestBody.encode())))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        testContext.completeNow();
      })));
  }
}
