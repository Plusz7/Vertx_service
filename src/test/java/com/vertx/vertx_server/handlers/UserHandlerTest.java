package com.vertx.vertx_server.handlers;

import com.vertx.vertx_server.handler.UserHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class UserHandlerTest {
  @Mock
  private MongoClient mockMongoClient;

  @Mock
  private JWTAuth mockJWTAuth;

  @Mock
  private RoutingContext mockRoutingContext;

  @Mock
  private HttpServerResponse mockResponse;

  @Captor
  private ArgumentCaptor<JsonObject> userCaptor;

  @Captor
  private ArgumentCaptor<Integer> statusCodeCaptor;

  private UserHandler userHandler;


  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    userHandler = new UserHandler(mockMongoClient, mockJWTAuth);

    when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
  }

  @AfterEach
  public void tearDown() {
    Mockito.clearAllCaches();
  }

  @Test
  public void testHandleUserRegister() {
    JsonObject mockUserJson = new JsonObject()
      .put("login", "test@sometest.com")
      .put("password", "testpass");

    when(mockRoutingContext.getBodyAsJson()).thenReturn(mockUserJson);

    userHandler.handleRegister(mockRoutingContext);

    verify(mockMongoClient, times(1)).save(eq("users"), userCaptor.capture(), any());
    verifyNoMoreInteractions(mockMongoClient, mockJWTAuth, mockResponse);

    JsonObject capturedUser = userCaptor.getValue();

    assertNotNull(capturedUser.getString("id"));
    assertEquals("test@sometest.com", capturedUser.getString("login"));
    assertTrue(BCrypt.checkpw("testpass", capturedUser.getString("password")));
  }

  @Test
  public void testHandleLogin() {

    String testLogin = "test@sometest.com";
    String testPassword = "testpass";
    JsonObject mockUser = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("login", testLogin)
      .put("password", BCrypt.hashpw(testPassword, BCrypt.gensalt()));

    JsonObject expectedQuery = new JsonObject().put("login", testLogin);

    JsonObject mockUserJson = new JsonObject()
      .put("login", testLogin)
      .put("password", testPassword);

    when(mockRoutingContext.getBodyAsJson()).thenReturn(mockUserJson);
    when(mockRoutingContext.response()).thenReturn(mockResponse);
    when(mockMongoClient.findOne(eq("users"), eq(expectedQuery), eq(null), any()))
      .thenAnswer(invocation -> {
        Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
        handler.handle(Future.succeededFuture(mockUser));
        return null;
      });
    when(mockJWTAuth.generateToken(any(), any())).thenReturn("mockedToken");

    userHandler.handleLogin(mockRoutingContext);

    verify(mockResponse, times(1)).putHeader(eq("Content-Type"), eq("application/json"));
    verify(mockResponse, times(1)).end((String) argThat(argument -> {
      JsonObject response = new JsonObject((String) argument);
      String token = response.getString("token");
      assertNotNull(token, "Token should not be null");
      assertEquals("mockedToken", token, "Token should match the expected value");
      return true;
    }));
    verify(mockMongoClient, times(1)).findOne(eq("users"), eq(expectedQuery), eq(null), any());
  }
}
