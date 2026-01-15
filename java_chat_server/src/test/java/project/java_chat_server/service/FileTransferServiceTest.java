package project.java_chat_server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import project.java_chat_server.domain.FileTransferSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileTransferServiceTest {

    private FileTransferService fileTransferService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileTransferService = new FileTransferService(tempDir.toString());
        fileTransferService.init();
    }

    @Test
    void testStartFileTransfer_Success() throws IOException {
        int clientId = 1;
        String fileName = "test.txt";
        long fileSize = 1024;

        assertDoesNotThrow(() -> {
            fileTransferService.startFileTransfer(clientId, fileName, fileSize);
        });

        FileTransferSession session = fileTransferService.getSession(clientId);
        assertNotNull(session);
        assertEquals(fileName, session.getFileName());
        assertEquals(fileSize, session.getFileSize());
        assertFalse(session.isCompleted());
    }

    @Test
    void testStartFileTransfer_Duplicate() throws IOException {
        int clientId = 1;
        String fileName = "test.txt";
        long fileSize = 1024;

        fileTransferService.startFileTransfer(clientId, fileName, fileSize);

        assertThrows(IOException.class, () -> {
            fileTransferService.startFileTransfer(clientId, "another.txt", 2048);
        });
    }

    @Test
    void testProcessFileChunk_Success() throws IOException {
        int clientId = 1;
        String fileName = "test.txt";
        long fileSize = 100;
        
        fileTransferService.startFileTransfer(clientId, fileName, fileSize);
        
        byte[] chunk1 = new byte[50];
        byte[] chunk2 = new byte[50];
        
        fileTransferService.processFileChunk(clientId, chunk1);
        fileTransferService.processFileChunk(clientId, chunk2);
        
        FileTransferSession session = fileTransferService.getSession(clientId);
        assertTrue(session.isCompleted());
        assertEquals(fileSize, session.getReceivedBytes());
    }

    @Test
    void testProcessFileChunk_NoSession() {
        int clientId = 999;
        byte[] chunk = new byte[100];

        assertThrows(IOException.class, () -> {
            fileTransferService.processFileChunk(clientId, chunk);
        });
    }

    @Test
    void testEndFileTransfer_Success() throws Exception {
        int clientId = 1;
        String fileName = "test.txt";
        byte[] fileContent = "Hello, World!".getBytes();
        long fileSize = fileContent.length;
        
        // Start transfer
        fileTransferService.startFileTransfer(clientId, fileName, fileSize);
        
        // Process chunk
        fileTransferService.processFileChunk(clientId, fileContent);
        
        // Calculate checksum
        String checksum = org.apache.commons.codec.digest.DigestUtils.sha256Hex(fileContent);
        
        // End transfer
        FileTransferSession session = fileTransferService.endFileTransfer(clientId, checksum);
        assertNotNull(session);
        assertEquals(fileName, session.getFileName());
        
        // Session should be removed
        assertNull(fileTransferService.getSession(clientId));
    }

    @Test
    void testEndFileTransfer_InvalidChecksum() throws Exception {
        int clientId = 1;
        String fileName = "test.txt";
        byte[] fileContent = "Hello, World!".getBytes();
        long fileSize = fileContent.length;
        
        fileTransferService.startFileTransfer(clientId, fileName, fileSize);
        fileTransferService.processFileChunk(clientId, fileContent);
        
        String wrongChecksum = "wrong_checksum";
        
        assertThrows(IOException.class, () -> {
            fileTransferService.endFileTransfer(clientId, wrongChecksum);
        });
        
        // Session should be removed after failed checksum
        assertNull(fileTransferService.getSession(clientId));
    }

    @Test
    void testEndFileTransfer_Incomplete() throws Exception {
        int clientId = 1;
        String fileName = "test.txt";
        long fileSize = 100;
        
        fileTransferService.startFileTransfer(clientId, fileName, fileSize);
        fileTransferService.processFileChunk(clientId, new byte[50]); // Only 50 bytes
        
        assertThrows(IOException.class, () -> {
            fileTransferService.endFileTransfer(clientId, "checksum");
        });
        
        // Session should be removed and file deleted
        assertNull(fileTransferService.getSession(clientId));
    }

    @Test
    void testEndFileTransfer_NoSession() {
        int clientId = 999;
        
        assertThrows(IOException.class, () -> {
            fileTransferService.endFileTransfer(clientId, "checksum");
        });
    }

    @Test
    void testCancelFileTransfer_Success() throws IOException {
        int clientId = 1;
        String fileName = "test.txt";
        long fileSize = 1024;
        
        fileTransferService.startFileTransfer(clientId, fileName, fileSize);
        FileTransferSession session = fileTransferService.getSession(clientId);
        Path filePath = session.getFilePath();
        
        // Cancel transfer
        fileTransferService.cancelFileTransfer(clientId, "Test cancellation");
        
        // Session should be removed
        assertNull(fileTransferService.getSession(clientId));
        
        // File should be deleted
        assertFalse(Files.exists(filePath));
    }

    @Test
    void testCancelFileTransfer_NoSession() {
        int clientId = 999;
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            fileTransferService.cancelFileTransfer(clientId, "No session");
        });
    }

    @Test
    void testGetSession_Exists() throws IOException {
        int clientId = 1;
        fileTransferService.startFileTransfer(clientId, "test.txt", 100);
        
        FileTransferSession session = fileTransferService.getSession(clientId);
        assertNotNull(session);
    }

    @Test
    void testGetSession_NotExists() {
        int clientId = 999;
        
        FileTransferSession session = fileTransferService.getSession(clientId);
        assertNull(session);
    }

    @Test
    void testMultipleClients() throws IOException {
        fileTransferService.startFileTransfer(1, "file1.txt", 100);
        fileTransferService.startFileTransfer(2, "file2.txt", 200);
        fileTransferService.startFileTransfer(3, "file3.txt", 300);
        
        assertNotNull(fileTransferService.getSession(1));
        assertNotNull(fileTransferService.getSession(2));
        assertNotNull(fileTransferService.getSession(3));
    }
}
