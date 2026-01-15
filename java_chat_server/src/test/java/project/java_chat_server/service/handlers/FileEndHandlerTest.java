package project.java_chat_server.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import project.java_chat_server.dto.file.FileEndRequest;
import project.java_chat_server.service.FileTransferService;
import project.java_chat_server.service.UserService;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.test_utils.TestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileEndHandlerTest {

    private FileEndHandler handler;
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
        handler = new FileEndHandler(fileTransferService, userService, objectMapper);
        testClient = TestUtils.createClientInfo(1, "127.0.0.1");
        
        // Login user first
        userService.login(testClient.socketFd, "testuser");
    }

    @Test
    void testHandle_Success() throws Exception {
        String fileName = "test.txt";
        byte[] fileContent = "Hello, World!".getBytes();
        long fileSize = fileContent.length;
        String checksum = DigestUtils.sha256Hex(fileContent);
        
        // Start transfer and process chunks
        fileTransferService.startFileTransfer(testClient.socketFd, fileName, fileSize);
        fileTransferService.processFileChunk(testClient.socketFd, fileContent);
        
        FileEndRequest request = new FileEndRequest(fileName, checksum);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_FILE_END, result.getDirectResponse().get().type());
        assertTrue(result.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_SERVER_NOTICE, result.getBroadcast().get().type());
        
        // Session should be removed
        assertNull(fileTransferService.getSession(testClient.socketFd));
    }

    @Test
    void testHandle_NotLoggedIn() throws Exception {
        ClientInfo unauthenticatedClient = TestUtils.createClientInfo(999, "127.0.0.1");
        FileEndRequest request = new FileEndRequest("test.txt", "checksum");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(unauthenticatedClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_InvalidChecksum() throws Exception {
        String fileName = "test.txt";
        byte[] fileContent = "Hello, World!".getBytes();
        long fileSize = fileContent.length;
        
        fileTransferService.startFileTransfer(testClient.socketFd, fileName, fileSize);
        fileTransferService.processFileChunk(testClient.socketFd, fileContent);
        
        FileEndRequest request = new FileEndRequest(fileName, "wrong_checksum");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
        
        // Session should be removed after checksum failure
        assertNull(fileTransferService.getSession(testClient.socketFd));
    }

    @Test
    void testHandle_IncompleteTransfer() throws Exception {
        String fileName = "test.txt";
        byte[] fileContent = "Hello, World!".getBytes();
        long fileSize = fileContent.length + 100; // Expect more than received
        
        fileTransferService.startFileTransfer(testClient.socketFd, fileName, fileSize);
        fileTransferService.processFileChunk(testClient.socketFd, fileContent);
        
        FileEndRequest request = new FileEndRequest(fileName, "checksum");
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
        
        // Session should be removed
        assertNull(fileTransferService.getSession(testClient.socketFd));
    }

    @Test
    void testHandle_NoSession() throws Exception {
        FileEndRequest request = new FileEndRequest("test.txt", "checksum");
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
    void testGetMessageType() {
        assertEquals(MessageType.MSG_TYPE_FILE_END, handler.getMessageType());
    }

    @Test
    void testHandle_LargeFile() throws Exception {
        String fileName = "large.txt";
        byte[] fileContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < fileContent.length; i++) {
            fileContent[i] = (byte) (i % 256);
        }
        long fileSize = fileContent.length;
        String checksum = DigestUtils.sha256Hex(fileContent);
        
        fileTransferService.startFileTransfer(testClient.socketFd, fileName, fileSize);
        
        // Process in chunks
        int chunkSize = 100 * 1024; // 100KB chunks
        for (int i = 0; i < fileContent.length; i += chunkSize) {
            int remaining = Math.min(chunkSize, fileContent.length - i);
            byte[] chunk = new byte[remaining];
            System.arraycopy(fileContent, i, chunk, 0, remaining);
            fileTransferService.processFileChunk(testClient.socketFd, chunk);
        }
        
        FileEndRequest request = new FileEndRequest(fileName, checksum);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        HandlerResult result = handler.handle(testClient, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_FILE_END, result.getDirectResponse().get().type());
    }
}
