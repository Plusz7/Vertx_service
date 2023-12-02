package com.vertx.vertx_server.handlers;

import com.vertx.vertx_server.handler.ItemHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ItemHandlerTest {

  @Mock
  private MongoClient mockMongoClient;

  @Mock
  private RoutingContext mockRoutingContext;

  @Mock
  private HttpServerResponse mockResponse;

  @Captor
  private ArgumentCaptor<JsonObject> itemCaptor;

  private ItemHandler itemHandler;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    itemHandler = new ItemHandler(mockMongoClient);
  }

  @AfterEach
  public void tearDown() {
    Mockito.clearAllCaches();
  }


  @Test
  public void handleAddItemTest() {
    String ownerId = "eb5c7783-b3e4-4466-b281-13acb9990565";
    JsonObject mockItemJson = new JsonObject().put("name", "testItem");
    JsonObject principalJson = new JsonObject().put("ownerId", ownerId);

    User mockUser = mock(User.class);

    when(mockUser.principal()).thenReturn(principalJson);
    when(mockRoutingContext.user()).thenReturn(mockUser);
    when(mockRoutingContext.getBodyAsJson()).thenReturn(mockItemJson);

    itemHandler.handleAddItem(mockRoutingContext);

    verify(mockMongoClient, times(1)).save(eq("items"), itemCaptor.capture(), any());

    JsonObject capturedItem = itemCaptor.getValue();

    assertNotNull(capturedItem.getString("id"));
    assertEquals(ownerId, capturedItem.getString("owner"));
  }

  @Test
  public void handleGetItemsTest() {
    String ownerId = "eb5c7783-b3e4-4466-b281-13acb9990565";
    JsonObject principalJson = new JsonObject().put("ownerId", ownerId);
    User mockUser = mock(User.class);
    JsonObject query = new JsonObject().put("owner", ownerId);

    JsonObject item1 = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("owner", ownerId)
      .put("name", "someTestName");
    List<JsonObject> items = Collections.singletonList(item1);

    when(mockUser.principal()).thenReturn(principalJson);
    when(mockRoutingContext.user()).thenReturn(mockUser);
    when(mockRoutingContext.response()).thenReturn(mockResponse);
    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
    when(mockMongoClient.find(eq("items"), eq(query), any())).thenAnswer( invocation -> {
        Handler <AsyncResult<List<JsonObject>>> handler = invocation.getArgument(2);
        handler.handle(Future.succeededFuture(items));
        return null;
      }
    );

    itemHandler.handleGetItems(mockRoutingContext);

    verify(mockMongoClient, times(1)).find(eq("items"), eq(query), any());
    verify(mockResponse, times(1)).end((String) argThat( argument -> {
      JsonArray responseArray = new JsonArray((String) argument);
      JsonObject responseObject = responseArray.getJsonObject(0);

      assertEquals(1, responseArray.size());
      assertEquals(item1.getString("id"), responseObject.getString("id"));
      assertEquals(item1.getString("owner"), responseObject.getString("owner"));
      assertEquals(item1.getString("name"), responseObject.getString("name"));
      return true;
    }));
  }

}
