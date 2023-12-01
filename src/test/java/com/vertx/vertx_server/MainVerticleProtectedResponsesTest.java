package com.vertx.vertx_server;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearAllCaches;

@ExtendWith(VertxExtension.class)
public class MainVerticleProtectedResponsesTest {
  @Mock
  private MongoClient mockClient;
  private String token;

  @BeforeEach
  void setup(Vertx vertx, VertxTestContext testContext) {
    // creating proper test context setup with JWT
    ConfigRetrieverOptions options = getConfigRetrieverOptions();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);
    configRetriever.getConfig(ar -> {
      if (ar.succeeded()) {
        JsonObject config = ar.result();
        JsonObject jwtConfig = config.getJsonObject("jwt");
        token = initJWTAuth(jwtConfig, vertx).generateToken(
          new JsonObject().put("ownerId", "eb5c7783-b3e4-4466-b281-13acb9990565"),
          new JWTOptions().setExpiresInSeconds(5)
        );

        vertx.deployVerticle(new MainVerticle(), testContext.succeedingThenComplete());
      }
    });
  }

  @AfterEach
  void teardown(Vertx vertx) {
    clearAllCaches();
    vertx.close();
    token = null;
  }

  @Test
  void testPostItemsWithJwt(Vertx vertx, VertxTestContext testContext) {
    JsonObject item = new JsonObject().put("name", "NewItem");

    HttpClient client = vertx.createHttpClient();
    client.request(HttpMethod.POST, 3000, "localhost", "/items")
      .compose(req -> req.
        putHeader("Authorization", "Bearer " + token)
        .putHeader("content-type", "application/json")
        .send(Buffer.buffer(item.encode()))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(201, response.statusCode());
        testContext.completeNow();
        }))));
  }

  @Test
  void testGetUsersItemsWithJwt(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    client.request(HttpMethod.GET, 3000, "localhost", "/items")
      .compose(req -> req.putHeader("Authorization", "Bearer " + token)
        .putHeader("content-type", "application/json")
        .send())
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  private JWTAuth initJWTAuth(JsonObject jwtConfig, Vertx vertx) {
    return JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer(jwtConfig.getString("secret"))
      ));
  }
    private static ConfigRetrieverOptions getConfigRetrieverOptions() {
      ConfigStoreOptions fileStore = new ConfigStoreOptions()
        .setType("file")
        .setFormat("json")
        .setConfig(new JsonObject().put("path", "config.json"));
      return new ConfigRetrieverOptions().addStore(fileStore);
    }
}
