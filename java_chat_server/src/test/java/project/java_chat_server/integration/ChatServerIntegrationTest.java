package project.java_chat_server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.Timeout;
import project.java_chat_server.dto.chat.ChatTextRequest;
import project.java_chat_server.dto.user.UserLoginRequest;
import project.java_chat_server.service.UserService;
import project.java_chat_server.service.handlers.ChatTextHandler;
import project.java_chat_server.service.handlers.LoginRequestHandler;
import project.java_chat_server.service.handlers.MessageHandler;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.wrapper_library.ChatServer;
import project.java_chat_server.wrapper_library.callbacks.ServerOnClientConnectedCallback;
import project.java_chat_server.wrapper_library.callbacks.ServerOnClientDisconnectedCallback;
import project.java_chat_server.wrapper_library.callbacks.ServerOnCompleteMessageReceivedCallback;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.test_utils.TestUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs({OS.MAC, OS.LINUX})
class ChatServerIntegrationTest {

    private ChatServer server;
    private UserService userService;
    private ObjectMapper objectMapper;
    private List<MessageHandler> handlers;
    private static final int TEST_PORT = 9999;
    private static final int MAX_CLIENTS = 10;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Support Instant serialization
        handlers = Arrays.asList(
                new LoginRequestHandler(userService, objectMapper),
                new ChatTextHandler(objectMapper, userService)
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testServerCreation() {
        assertDoesNotThrow(() -> {
            server = new ChatServer(TEST_PORT, MAX_CLIENTS);
        });
        assertNotNull(server);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testServerCallbacks_Registration() {
        server = new ChatServer(TEST_PORT, MAX_CLIENTS);

        AtomicInteger connectCount = new AtomicInteger(0);
        AtomicInteger disconnectCount = new AtomicInteger(0);
        AtomicInteger messageCount = new AtomicInteger(0);

        ServerOnClientConnectedCallback onConnect = (userData, client) -> {
            connectCount.incrementAndGet();
        };

        ServerOnClientDisconnectedCallback onDisconnect = (userData, client) -> {
            disconnectCount.incrementAndGet();
        };

        ServerOnCompleteMessageReceivedCallback onMessage = (userData, client, msgType, payload, len) -> {
            messageCount.incrementAndGet();
        };

        assertDoesNotThrow(() -> {
            server.setOnConnectListener(onConnect);
            server.setOnDisconnectListener(onDisconnect);
            server.setOnMessageListener(onMessage);
        });
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMultipleHandlers_Processing() throws Exception {
        server = new ChatServer(TEST_PORT, MAX_CLIENTS);

        CountDownLatch loginLatch = new CountDownLatch(1);
        CountDownLatch chatLatch = new CountDownLatch(1);

        ServerOnCompleteMessageReceivedCallback onMessage = (userData, client, msgType, payload, len) -> {
            MessageHandler handler = handlers.stream()
                    .filter(h -> h.getMessageType().getValue() == msgType)
                    .findFirst()
                    .orElse(null);

            if (handler != null) {
                    byte[] payloadArray = new byte[(int) len.longValue()];
                    payload.read(0, payloadArray, 0, payloadArray.length);
                    HandlerResult result = handler.handle(client, payloadArray);

                if (msgType == MessageType.MSG_TYPE_USER_LOGIN_REQUEST.getValue()) {
                    loginLatch.countDown();
                } else if (msgType == MessageType.MSG_TYPE_CHAT_TEXT.getValue()) {
                    chatLatch.countDown();
                }
            }
        };

        server.setOnMessageListener(onMessage);

        // Start server
        assertDoesNotThrow(() -> server.start());

        // Wait a bit for server to be ready
        Thread.sleep(500);

        // Test login handler
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        UserLoginRequest loginRequest = new UserLoginRequest("testuser");
        byte[] loginPayload = objectMapper.writeValueAsBytes(loginRequest);
        Memory loginMemory = new Memory(loginPayload.length);
        loginMemory.write(0, loginPayload, 0, loginPayload.length);
        onMessage.invoke(null, client, MessageType.MSG_TYPE_USER_LOGIN_REQUEST.getValue(), 
                loginMemory, new com.sun.jna.NativeLong(loginPayload.length));

        assertTrue(loginLatch.await(2, TimeUnit.SECONDS), "Login handler should be called");

        // Test chat handler
        ChatTextRequest chatRequest = new ChatTextRequest("Hello, World!");
        byte[] chatPayload = objectMapper.writeValueAsBytes(chatRequest);
        Memory chatMemory = new Memory(chatPayload.length);
        chatMemory.write(0, chatPayload, 0, chatPayload.length);
        onMessage.invoke(null, client, MessageType.MSG_TYPE_CHAT_TEXT.getValue(), 
                chatMemory, new com.sun.jna.NativeLong(chatPayload.length));

        assertTrue(chatLatch.await(2, TimeUnit.SECONDS), "Chat handler should be called");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testHandlerResult_ResponseAndBroadcast() throws Exception {
        LoginRequestHandler handler = new LoginRequestHandler(userService, objectMapper);

        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        UserLoginRequest request = new UserLoginRequest("testuser");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(client, payload);

        assertTrue(result.getDirectResponse().isPresent(), "Should have direct response");
        assertEquals(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, 
                result.getDirectResponse().get().type());
        assertTrue(result.getBroadcast().isPresent(), "Should have broadcast");
        assertEquals(MessageType.MSG_TYPE_USER_JOIN_NOTICE, 
                result.getBroadcast().get().type());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testUserService_Integration() throws Exception {
        // Test user service with handlers
        LoginRequestHandler loginHandler = new LoginRequestHandler(userService, objectMapper);

        ClientInfo client1 = TestUtils.createClientInfo(1, "127.0.0.1");
        UserLoginRequest request1 = new UserLoginRequest("user1");
        byte[] payload1 = objectMapper.writeValueAsBytes(request1);

        HandlerResult result1 = loginHandler.handle(client1, payload1);
        assertTrue(result1.getDirectResponse().isPresent());
        assertTrue(userService.isLoggedIn(1));

        ClientInfo client2 = TestUtils.createClientInfo(2, "127.0.0.1");
        UserLoginRequest request2 = new UserLoginRequest("user2");
        byte[] payload2 = objectMapper.writeValueAsBytes(request2);

        HandlerResult result2 = loginHandler.handle(client2, payload2);
        assertTrue(result2.getDirectResponse().isPresent());
        assertTrue(userService.isLoggedIn(2));

        // Test chat with logged in users
        ChatTextHandler chatHandler = new ChatTextHandler(objectMapper, userService);
        ChatTextRequest chatRequest = new ChatTextRequest("Hello");
        byte[] chatPayload = objectMapper.writeValueAsBytes(chatRequest);

        HandlerResult chatResult = chatHandler.handle(client1, chatPayload);
        assertTrue(chatResult.getBroadcast().isPresent());

        // Test logout
        String nickname = userService.logout(1);
        assertEquals("user1", nickname);
        assertFalse(userService.isLoggedIn(1));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testErrorHandling_InvalidJson() {
        LoginRequestHandler handler = new LoginRequestHandler(userService, objectMapper);
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        byte[] invalidPayload = "invalid json".getBytes(StandardCharsets.UTF_8);

        HandlerResult result = handler.handle(client, invalidPayload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                result.getDirectResponse().get().type());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testErrorHandling_UnauthenticatedChat() {
        ChatTextHandler handler = new ChatTextHandler(objectMapper, userService);
        ClientInfo client = TestUtils.createClientInfo(999, "127.0.0.1");

        try {
            ChatTextRequest request = new ChatTextRequest("Hello");
            byte[] payload = objectMapper.writeValueAsBytes(request);

            HandlerResult result = handler.handle(client, payload);

            assertTrue(result.getDirectResponse().isPresent());
            assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, 
                    result.getDirectResponse().get().type());
        } catch (Exception e) {
            fail("Should handle unauthenticated client gracefully");
        }
    }
}
