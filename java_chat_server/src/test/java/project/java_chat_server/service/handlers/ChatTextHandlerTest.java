package project.java_chat_server.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import project.java_chat_server.dto.chat.ChatTextRequest;
import project.java_chat_server.service.UserService;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.test_utils.TestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ChatTextHandlerTest {

    private ChatTextHandler handler;
    private UserService userService;
    private ObjectMapper objectMapper;
    private ClientInfo testClient;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Support Instant serialization
        handler = new ChatTextHandler(objectMapper, userService);
        testClient = TestUtils.createClientInfo(1, "127.0.0.1");

        // Login user first
        userService.login(testClient.socketFd, "testuser");
    }

    @Test
    void testHandle_Success() throws Exception {
        ChatTextRequest request = new ChatTextRequest("Hello, World!");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertFalse(result.getDirectResponse().isPresent());
        assertTrue(result.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_CHAT_TEXT, result.getBroadcast().get().type());
    }

    @Test
    void testHandle_NotLoggedIn() throws Exception {
        ClientInfo unauthenticatedClient = TestUtils.createClientInfo(999, "127.0.0.1");
        ChatTextRequest request = new ChatTextRequest("Hello");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(unauthenticatedClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                result.getDirectResponse().get().type());
        assertFalse(result.getBroadcast().isPresent());
    }

    @Test
    void testHandle_EmptyMessage() throws Exception {
        ChatTextRequest request = new ChatTextRequest("");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertFalse(result.getDirectResponse().isPresent());
        assertFalse(result.getBroadcast().isPresent());
    }

    @Test
    void testHandle_BlankMessage() throws Exception {
        ChatTextRequest request = new ChatTextRequest("   ");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertFalse(result.getDirectResponse().isPresent());
        assertFalse(result.getBroadcast().isPresent());
    }

    @Test
    void testHandle_NullMessage() throws Exception {
        // Create JSON with null message
        String json = "{\"message\":null}";
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);

        HandlerResult result = handler.handle(testClient, payload);

        assertFalse(result.getDirectResponse().isPresent());
        assertFalse(result.getBroadcast().isPresent());
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
        assertEquals(MessageType.MSG_TYPE_CHAT_TEXT, handler.getMessageType());
    }

    @Test
    void testHandle_LongMessage() throws Exception {
        String longMessage = "a".repeat(10000);
        ChatTextRequest request = new ChatTextRequest(longMessage);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_CHAT_TEXT, result.getBroadcast().get().type());
    }

    @Test
    void testHandle_MessageWithSpecialCharacters() throws Exception {
        ChatTextRequest request = new ChatTextRequest("Hello! @#$%^&*()_+-=[]{}|;':\",./<>?");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_CHAT_TEXT, result.getBroadcast().get().type());
    }
}
