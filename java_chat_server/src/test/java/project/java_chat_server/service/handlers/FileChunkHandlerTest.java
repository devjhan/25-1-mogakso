package project.java_chat_server.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import project.java_chat_server.service.FileTransferService;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.test_utils.TestUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileChunkHandlerTest {

    private FileChunkHandler handler;
    private FileTransferService fileTransferService;
    private ObjectMapper objectMapper;
    private ClientInfo testClient;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        fileTransferService = new FileTransferService(tempDir.toString());
        fileTransferService.init();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Support Instant serialization
        handler = new FileChunkHandler(fileTransferService, objectMapper);
        testClient = TestUtils.createClientInfo(1, "127.0.0.1");
    }

    @Test
    void testHandle_Success() throws Exception {
        // Start file transfer first
        fileTransferService.startFileTransfer(testClient.socketFd, "test.txt", 100);
        
        byte[] chunk = new byte[50];
        HandlerResult result = handler.handle(testClient, chunk);

        assertFalse(result.getDirectResponse().isPresent());
        assertFalse(result.getBroadcast().isPresent());
        
        // Verify chunk was processed
        assertNotNull(fileTransferService.getSession(testClient.socketFd));
        assertEquals(50, fileTransferService.getSession(testClient.socketFd).getReceivedBytes());
    }

    @Test
    void testHandle_NoSession() {
        byte[] chunk = new byte[100];

        HandlerResult result = handler.handle(testClient, chunk);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }

    @Test
    void testHandle_MultipleChunks() throws Exception {
        fileTransferService.startFileTransfer(testClient.socketFd, "test.txt", 200);
        
        byte[] chunk1 = new byte[50];
        byte[] chunk2 = new byte[100];
        byte[] chunk3 = new byte[50];

        handler.handle(testClient, chunk1);
        handler.handle(testClient, chunk2);
        handler.handle(testClient, chunk3);

        var session = fileTransferService.getSession(testClient.socketFd);
        assertNotNull(session);
        assertTrue(session.isCompleted());
        assertEquals(200, session.getReceivedBytes());
    }

    @Test
    void testHandle_EmptyChunk() throws Exception {
        fileTransferService.startFileTransfer(testClient.socketFd, "test.txt", 0);
        
        byte[] emptyChunk = new byte[0];
        
        HandlerResult result = handler.handle(testClient, emptyChunk);
        
        // Empty chunk should be processed without error
        assertFalse(result.getDirectResponse().isPresent());
    }

    @Test
    void testHandle_LargeChunk() throws Exception {
        fileTransferService.startFileTransfer(testClient.socketFd, "test.txt", 1024 * 1024); // 1MB
        
        byte[] largeChunk = new byte[512 * 1024]; // 512KB
        HandlerResult result = handler.handle(testClient, largeChunk);

        assertFalse(result.getDirectResponse().isPresent());
        
        var session = fileTransferService.getSession(testClient.socketFd);
        assertEquals(512 * 1024, session.getReceivedBytes());
    }

    @Test
    void testGetMessageType() {
        assertEquals(MessageType.MSG_TYPE_FILE_CHUNK, handler.getMessageType());
    }

    @Test
    void testHandle_IOException() throws Exception {
        fileTransferService.startFileTransfer(testClient.socketFd, "test.txt", 100);
        
        // Cancel the transfer to make file unavailable
        fileTransferService.cancelFileTransfer(testClient.socketFd, "test");
        
        // Now try to process chunk - should fail
        byte[] chunk = new byte[50];
        HandlerResult result = handler.handle(testClient, chunk);
        
        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, result.getDirectResponse().get().type());
    }
}
