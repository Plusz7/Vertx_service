package com.vertx.vertx_server;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(VertxExtension.class)
public class MainVerticleResponsesTest {
  private static final String LOCALHOST = "localhost";
  private static final String REGISTER_ENDPOINT = "/register";
  private static final String CONTENT_TYPE = "content-type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String LOGIN = "login";
  private static final String PASSWORD = "password";

  private Vertx vertx;
  @Mock
  private MongoClient mockMongoClient;


  @BeforeEach
  void setup(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    MockitoAnnotations.openMocks(this);
    vertx.deployVerticle(new MainVerticle(mockMongoClient), testContext.succeedingThenComplete());
  }

  @AfterEach
  void teardown() {
    Mockito.clearAllCaches();
    vertx.close();
  }

  @Test
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
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void httpServerCheckRegisterSuccessResponse(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    JsonObject requestBody = new JsonObject()
      .put(LOGIN, "testUser@mail.com")
      .put(PASSWORD, "testPass");

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(null));
      return null;
    }).when(mockMongoClient).findOne(eq("users"), any(JsonObject.class),eq(null), any());

    doAnswer(invocation -> {
      Handler<AsyncResult<Void>> handler = invocation.getArgument(2);
      handler.handle(Future.succeededFuture());
      return null;
    }).when(mockMongoClient).save(eq("users"), any(JsonObject.class), any());

    client.request(HttpMethod.POST, 3000, LOCALHOST, REGISTER_ENDPOINT)
      .compose(req -> req.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .send(Buffer.buffer(requestBody.encode())))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(201, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
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
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
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
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
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

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void httpServerCheckUserAlreadyExistsResponse(Vertx vertx, VertxTestContext testContext) {
      HttpClient client = vertx.createHttpClient();
      JsonObject requestBody = new JsonObject()
        .put(LOGIN, "testUser@mail.com")
        .put(PASSWORD, "testPass");

    doAnswer(invocation -> {
      Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
      handler.handle(Future.succeededFuture(new JsonObject()));
      return null;
    }).when(mockMongoClient).findOne(eq("users"), any(JsonObject.class),eq(null), any());

      client.request(HttpMethod.POST, 3000, LOCALHOST, REGISTER_ENDPOINT)
        .compose(req -> req.putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .send(Buffer.buffer(requestBody.encode())))
        .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
          assertEquals(400, response.statusCode());
          testContext.completeNow();
        })));
  }
}
