package project.java_chat_server.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import project.java_chat_server.dto.file.FileStartRequest;
import project.java_chat_server.service.FileTransferService;
import project.java_chat_server.service.UserService;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.test_utils.TestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStartHandlerTest {

    private FileStartHandler handler;
    private FileTransferService fileTransferService;
    private UserService userService;
    private ObjectMapper objectMapper;
    private ClientInfo testClient;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        fileTransferService = new FileTransferService(tempDir.toString());
        fileTransferService.init();
        userService = new UserService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Support Instant serialization
        handler = new FileStartHandler(fileTransferService, userService, objectMapper);
        testClient = TestUtils.createClientInfo(1, "127.0.0.1");
        
        // Login user first
        userService.login(testClient.socketFd, "testuser");
    }

    @Test
    void testHandle_Success() throws Exception {
        FileStartRequest request = new FileStartRequest("test.txt", 1024);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_SERVER_NOTICE, result.getDirectResponse().get().type());
        assertTrue(result.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_FILE_INFO, result.getBroadcast().get().type());
        
        // Verify session was created
        assertNotNull(fileTransferService.getSession(testClient.socketFd));
    }

    @Test
    void testHandle_NotLoggedIn() throws Exception {
        ClientInfo unauthenticatedClient = TestUtils.createClientInfo(999, "127.0.0.1");
        FileStartRequest request = new FileStartRequest("test.txt", 1024);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(unauthenticatedClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
        assertFalse(fileTransferService.getSession(unauthenticatedClient.socketFd) != null);
    }

    @Test
    void testHandle_EmptyFileName() throws Exception {
        FileStartRequest request = new FileStartRequest("", 1024);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_BlankFileName() throws Exception {
        FileStartRequest request = new FileStartRequest("   ", 1024);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_ZeroFileSize() throws Exception {
        FileStartRequest request = new FileStartRequest("test.txt", 0);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_NegativeFileSize() throws Exception {
        FileStartRequest request = new FileStartRequest("test.txt", -1);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_InvalidJson() {
        byte[] invalidPayload = "invalid json".getBytes(StandardCharsets.UTF_8);

        HandlerResult result = handler.handle(testClient, invalidPayload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_EmptyPayload() {
        byte[] emptyPayload = new byte[0];

        HandlerResult result = handler.handle(testClient, emptyPayload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_DuplicateTransfer() throws Exception {
        FileStartRequest request = new FileStartRequest("test.txt", 1024);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        handler.handle(testClient, payload);
        
        // Try to start another transfer for same client
        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testGetMessageType() {
        assertEquals(MessageType.MSG_TYPE_FILE_INFO, handler.getMessageType());
    }
}
