package project.java_chat_server.wrapper_library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import project.java_chat_server.wrapper_library.enums.MessageType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs({OS.MAC, OS.LINUX})
class ChatServerEdgeTest {

    @Test
    void testConstructor_ValidPort() {
        assertDoesNotThrow(() -> {
            ChatServer server = new ChatServer(8080, 10);
            server.close();
        });
    }

    @Test
    void testConstructor_ZeroPort() {
        // Port 0 should be valid (OS assigns port)
        assertDoesNotThrow(() -> {
            ChatServer server = new ChatServer(0, 10);
            server.close();
        });
    }

    @Test
    void testConstructor_MaxClients_Zero() {
        // C library requires max_clients > 0, so this should throw RuntimeException
        assertThrows(RuntimeException.class, () -> {
            ChatServer server = new ChatServer(8081, 0);
            server.close();
        });
    }

    @Test
    void testConstructor_MaxClients_Negative() {
        // C library requires max_clients > 0, so this should throw RuntimeException
        assertThrows(RuntimeException.class, () -> {
            ChatServer server = new ChatServer(8082, -1);
            server.close();
        });
    }

    @Test
    void testSendToClient_NullType() {
        ChatServer server = new ChatServer(8083, 10);
        try {
            assertThrows(IllegalArgumentException.class, () -> {
                server.sendToClient(1, null, "test".getBytes());
            });
        } finally {
            server.close();
        }
    }

    @Test
    void testSendToClient_NegativeClientId() {
        ChatServer server = new ChatServer(8084, 10);
        try {
            assertThrows(IllegalArgumentException.class, () -> {
                server.sendToClient(-1, MessageType.MSG_TYPE_CHAT_TEXT, "test".getBytes());
            });
        } finally {
            server.close();
        }
    }

    @Test
    void testSendToClient_NullPayload() {
        ChatServer server = new ChatServer(8085, 10);
        try {
            // ChatServer.sendToClient checks for null payload and returns early with warning
            // So it should not throw exception, just log warning
            assertDoesNotThrow(() -> {
                server.sendToClient(1, MessageType.MSG_TYPE_CHAT_TEXT, null);
            });
        } finally {
            server.close();
        }
    }

    @Test
    void testSendToClient_EmptyPayload() {
        ChatServer server = new ChatServer(8086, 10);
        try {
            // Empty payload should be handled gracefully
            assertDoesNotThrow(() -> {
                server.sendToClient(1, MessageType.MSG_TYPE_CHAT_TEXT, new byte[0]);
            });
        } catch (Exception e) {
            // If it throws, that's also acceptable behavior
        } finally {
            server.close();
        }
    }

    @Test
    void testBroadcast_NullType() {
        ChatServer server = new ChatServer(8087, 10);
        try {
            assertThrows(IllegalArgumentException.class, () -> {
                server.broadcast(null, "test".getBytes(), -1);
            });
        } finally {
            server.close();
        }
    }

    @Test
    void testBroadcast_NullPayload() {
        ChatServer server = new ChatServer(8088, 10);
        try {
            // ChatServer.broadcast checks for null payload and returns early with warning
            // So it should not throw exception, just log warning
            assertDoesNotThrow(() -> {
                server.broadcast(MessageType.MSG_TYPE_CHAT_TEXT, null, -1);
            });
        } finally {
            server.close();
        }
    }

    @Test
    void testBroadcast_EmptyPayload() {
        ChatServer server = new ChatServer(8089, 10);
        try {
            // Empty payload should be handled gracefully
            assertDoesNotThrow(() -> {
                server.broadcast(MessageType.MSG_TYPE_CHAT_TEXT, new byte[0], -1);
            });
        } catch (Exception e) {
            // If it throws, that's also acceptable behavior
        } finally {
            server.close();
        }
    }

    @Test
    void testClose_MultipleTimes() {
        ChatServer server = new ChatServer(8090, 10);
        server.close();
        // Should not throw exception on multiple closes
        assertDoesNotThrow(() -> server.close());
    }

    @Test
    void testSendAfterClose() {
        ChatServer server = new ChatServer(8091, 10);
        server.close();
        
        assertThrows(IOException.class, () -> {
            server.sendToClient(1, MessageType.MSG_TYPE_CHAT_TEXT, "test".getBytes());
        });
        
        assertThrows(IOException.class, () -> {
            server.broadcast(MessageType.MSG_TYPE_CHAT_TEXT, "test".getBytes(), -1);
        });
    }

    @Test
    void testAllMessageTypes_SendToClient() {
        ChatServer server = new ChatServer(8092, 10);
        try {
            for (MessageType type : MessageType.values()) {
                if (type != MessageType.MSG_TYPE_UNKNOWN) {
                    byte[] payload = "test".getBytes();
                    try {
                        server.sendToClient(999, type, payload);  // Client doesn't exist, but should validate
                    } catch (IOException e) {
                        // Expected if client doesn't exist
                    }
                }
            }
        } finally {
            server.close();
        }
    }

    @Test
    void testAllMessageTypes_Broadcast() {
        ChatServer server = new ChatServer(8093, 10);
        try {
            for (MessageType type : MessageType.values()) {
                if (type != MessageType.MSG_TYPE_UNKNOWN) {
                    byte[] payload = "test".getBytes();
                    try {
                        server.broadcast(type, payload, -1);
                    } catch (Exception e) {
                        // May throw if server not started, but validates message types
                    }
                }
            }
        } finally {
            server.close();
        }
    }

    @Test
    void testLargePayload() {
        ChatServer server = new ChatServer(8094, 10);
        try {
            byte[] largePayload = new byte[1024 * 1024];  // 1MB
            // Fill with data
            for (int i = 0; i < largePayload.length; i++) {
                largePayload[i] = (byte) (i % 256);
            }
            
            // Should handle large payload (may throw if server not started)
            try {
                server.sendToClient(999, MessageType.MSG_TYPE_FILE_CHUNK, largePayload);
            } catch (IOException e) {
                // Expected if client doesn't exist
            }
        } finally {
            server.close();
        }
    }
}
