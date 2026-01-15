package project.java_chat_server.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import project.java_chat_server.dto.user.UserLoginRequest;
import project.java_chat_server.service.UserService;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.test_utils.TestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestHandlerTest {

    private LoginRequestHandler handler;
    private UserService userService;
    private ObjectMapper objectMapper;
    private ClientInfo testClient;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Support Instant serialization
        handler = new LoginRequestHandler(userService, objectMapper);
        testClient = TestUtils.createClientInfo(1, "127.0.0.1");
    }

    @Test
    void testHandle_Success() throws Exception {
        UserLoginRequest request = new UserLoginRequest("testuser");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, 
                result.getDirectResponse().get().type());
        assertTrue(result.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_USER_JOIN_NOTICE, 
                result.getBroadcast().get().type());
        assertTrue(userService.isLoggedIn(testClient.socketFd));
        assertEquals("testuser", userService.getNickname(testClient.socketFd).orElse(null));
    }

    @Test
    void testHandle_EmptyNickname() throws Exception {
        UserLoginRequest request = new UserLoginRequest("");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                result.getDirectResponse().get().type());
        assertFalse(result.getBroadcast().isPresent());
    }

    @Test
    void testHandle_BlankNickname() throws Exception {
        UserLoginRequest request = new UserLoginRequest("   ");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_InvalidNickname_TooShort() throws Exception {
        UserLoginRequest request = new UserLoginRequest("ab");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_DuplicateNickname() throws Exception {
        // Login first user
        UserLoginRequest request1 = new UserLoginRequest("testuser");
        byte[] payload1 = objectMapper.writeValueAsBytes(request1);
        handler.handle(testClient, payload1);

        // Try to login second user with same nickname
        ClientInfo client2 = TestUtils.createClientInfo(2, "127.0.0.1");
        HandlerResult result = handler.handle(client2, payload1);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                result.getDirectResponse().get().type());
        assertFalse(userService.isLoggedIn(client2.socketFd));
    }

    @Test
    void testHandle_InvalidJson() {
        byte[] invalidPayload = "invalid json".getBytes(StandardCharsets.UTF_8);

        HandlerResult result = handler.handle(testClient, invalidPayload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_EmptyPayload() {
        byte[] emptyPayload = new byte[0];

        HandlerResult result = handler.handle(testClient, emptyPayload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                result.getDirectResponse().get().type());
    }

    @Test
    void testGetMessageType() {
        assertEquals(MessageType.MSG_TYPE_USER_LOGIN_REQUEST, handler.getMessageType());
    }

    @Test
    void testHandle_TrimNickname() throws Exception {
        UserLoginRequest request = new UserLoginRequest("  testuser  ");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, 
                result.getDirectResponse().get().type());
        assertEquals("testuser", userService.getNickname(testClient.socketFd).orElse(null));
    }
}
