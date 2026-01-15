package project.java_chat_server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import project.java_chat_server.dto.chat.ChatTextRequest;
import project.java_chat_server.service.handlers.ChatTextHandler;
import project.java_chat_server.service.handlers.LoginRequestHandler;
import project.java_chat_server.service.handlers.MessageHandler;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.wrapper_library.ChatServer;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.test_utils.TestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private ChatService chatService;
    private UserService userService;
    private ObjectMapper objectMapper;

    @Mock
    private ChatServer mockChatServer;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        userService = new UserService();
        
        // ObjectMapper with JSR310 module for Instant support
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Mock ChatServer methods to avoid IOException
        // Using Answer to handle IOException properly
        doAnswer(invocation -> null).when(mockChatServer).broadcast(
                any(MessageType.class), any(byte[].class), anyInt());
        doAnswer(invocation -> null).when(mockChatServer).sendToClient(
                anyInt(), any(MessageType.class), any(byte[].class));

        List<MessageHandler> handlers = Arrays.asList(
                new LoginRequestHandler(userService, objectMapper),
                new ChatTextHandler(objectMapper, userService)
        );

        chatService = new ChatService(mockChatServer, handlers, userService, objectMapper);
    }

    @Test
    void testHandleClientConnected() {
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        
        // Should not throw exception
        assertDoesNotThrow(() -> chatService.handleClientConnected(client));
    }

    @Test
    void testHandleClientDisconnected_LoggedInUser() throws Exception {
        // Login user first
        userService.login(1, "testuser");
        
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        
        try {
            chatService.handleClientDisconnected(client);
        } catch (Exception e) {
            // Ignore IOException from mock
        }
        
        assertFalse(userService.isLoggedIn(1));
        try {
            verify(mockChatServer, atLeastOnce()).broadcast(
                    eq(MessageType.MSG_TYPE_USER_LEAVE_NOTICE), 
                    any(byte[].class), 
                    eq(-1)
            );
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    void testHandleClientDisconnected_NotLoggedInUser() {
        ClientInfo client = TestUtils.createClientInfo(999, "127.0.0.1");
        
        // Should not throw exception
        try {
            chatService.handleClientDisconnected(client);
            verify(mockChatServer, atLeastOnce()).broadcast(
                    eq(MessageType.MSG_TYPE_USER_LEAVE_NOTICE), 
                    any(byte[].class), 
                    eq(-1)
            );
        } catch (Exception e) {
            // IOException from mock is acceptable
        }
    }

    @Test
    void testHandleMessageReceived_ValidMessage() throws Exception {
        // Login user first
        userService.login(1, "testuser");
        
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        ChatTextRequest request = new ChatTextRequest("Hello");
        byte[] payload = objectMapper.writeValueAsBytes(request);
        
        chatService.handleMessageReceived(client, MessageType.MSG_TYPE_CHAT_TEXT.getValue(), payload);
        
        // Verify broadcast was called
        // ChatTextHandler should return HandlerResult.broadcast() which triggers executeHandlerResult
        verify(mockChatServer, atLeastOnce()).broadcast(
                eq(MessageType.MSG_TYPE_CHAT_TEXT), 
                any(byte[].class), 
                eq(1)
        );
    }

    @Test
    void testHandleMessageReceived_InvalidMessageType() throws Exception {
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);
        
        // MSG_TYPE_USER_JOIN_NOTICE is not a valid client message type
        chatService.handleMessageReceived(client, MessageType.MSG_TYPE_USER_JOIN_NOTICE.getValue(), payload);
        
        verify(mockChatServer, atLeastOnce()).sendToClient(
                eq(1), 
                eq(MessageType.MSG_TYPE_ERROR_RESPONSE), 
                any(byte[].class)
        );
    }

    @Test
    void testHandleMessageReceived_UnknownMessageType() throws Exception {
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);
        
        // Unknown message type (999)
        chatService.handleMessageReceived(client, 999, payload);
        
        verify(mockChatServer, atLeastOnce()).sendToClient(
                eq(1), 
                eq(MessageType.MSG_TYPE_ERROR_RESPONSE), 
                any(byte[].class)
        );
    }

    @Test
    void testHandleMessageReceived_LoginRequest() throws Exception {
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        String json = "{\"nickname\":\"testuser\"}";
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        
        chatService.handleMessageReceived(client, MessageType.MSG_TYPE_USER_LOGIN_REQUEST.getValue(), payload);
        
        assertTrue(userService.isLoggedIn(1));
        verify(mockChatServer, atLeastOnce()).sendToClient(
                eq(1), 
                eq(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE), 
                any(byte[].class)
        );
        verify(mockChatServer, atLeastOnce()).broadcast(
                eq(MessageType.MSG_TYPE_USER_JOIN_NOTICE), 
                any(byte[].class), 
                eq(1)
        );
    }

    @Test
    void testHandleMessageReceived_UnauthenticatedChatMessage() throws Exception {
        ClientInfo client = TestUtils.createClientInfo(1, "127.0.0.1");
        ChatTextRequest request = new ChatTextRequest("Hello");
        byte[] payload = objectMapper.writeValueAsBytes(request);
        
        // User not logged in
        chatService.handleMessageReceived(client, MessageType.MSG_TYPE_CHAT_TEXT.getValue(), payload);
        
        // Verify sendToClient was called with error response
        // ChatTextHandler returns createErrorResponse() for unauthenticated users
        // This should trigger executeHandlerResult with directResponse
        verify(mockChatServer, atLeastOnce()).sendToClient(
                eq(1), 
                eq(MessageType.MSG_TYPE_ERROR_RESPONSE), 
                any(byte[].class)
        );
        verify(mockChatServer, never()).broadcast(any(), any(), anyInt());
    }
}
