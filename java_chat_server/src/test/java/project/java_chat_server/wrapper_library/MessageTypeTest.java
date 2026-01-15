package project.java_chat_server.wrapper_library;

import org.junit.jupiter.api.Test;
import project.java_chat_server.wrapper_library.enums.MessageType;

import static org.junit.jupiter.api.Assertions.*;

class MessageTypeTest {

    @Test
    void testFromValue_ValidValues() {
        assertEquals(MessageType.MSG_TYPE_CHAT_TEXT, MessageType.fromValue(1));
        assertEquals(MessageType.MSG_TYPE_FILE_INFO, MessageType.fromValue(10));
        assertEquals(MessageType.MSG_TYPE_FILE_CHUNK, MessageType.fromValue(11));
        assertEquals(MessageType.MSG_TYPE_USER_LOGIN_REQUEST, MessageType.fromValue(100));
        assertEquals(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, MessageType.fromValue(101));
        assertEquals(MessageType.MSG_TYPE_USER_JOIN_NOTICE, MessageType.fromValue(200));
        assertEquals(MessageType.MSG_TYPE_ERROR_RESPONSE, MessageType.fromValue(500));
        assertEquals(MessageType.MSG_TYPE_PING, MessageType.fromValue(900));
        assertEquals(MessageType.MSG_TYPE_PONG, MessageType.fromValue(901));
    }

    @Test
    void testFromValue_InvalidValue() {
        assertEquals(MessageType.MSG_TYPE_UNKNOWN, MessageType.fromValue(999));
        assertEquals(MessageType.MSG_TYPE_UNKNOWN, MessageType.fromValue(-1));
        assertEquals(MessageType.MSG_TYPE_UNKNOWN, MessageType.fromValue(0));
    }

    @Test
    void testGetValue() {
        assertEquals(1, MessageType.MSG_TYPE_CHAT_TEXT.getValue());
        assertEquals(10, MessageType.MSG_TYPE_FILE_INFO.getValue());
        assertEquals(100, MessageType.MSG_TYPE_USER_LOGIN_REQUEST.getValue());
        assertEquals(500, MessageType.MSG_TYPE_ERROR_RESPONSE.getValue());
        assertEquals(900, MessageType.MSG_TYPE_PING.getValue());
        assertEquals(901, MessageType.MSG_TYPE_PONG.getValue());
    }

    @Test
    void testAllMessageTypes_HaveUniqueValues() {
        MessageType[] types = MessageType.values();
        long uniqueValueCount = java.util.Arrays.stream(types)
                .mapToInt(MessageType::getValue)
                .distinct()
                .count();
        assertEquals(types.length, uniqueValueCount, "All message types should have unique values");
    }

    @Test
    void testFromValue_AllKnownTypes() {
        for (MessageType type : MessageType.values()) {
            if (type != MessageType.MSG_TYPE_UNKNOWN) {
                assertEquals(type, MessageType.fromValue(type.getValue()),
                        "fromValue should return " + type + " for value " + type.getValue());
            }
        }
    }

    @Test
    void testFromValue_EdgeCases() {
        assertEquals(MessageType.MSG_TYPE_UNKNOWN, MessageType.fromValue(Integer.MAX_VALUE));
        assertEquals(MessageType.MSG_TYPE_UNKNOWN, MessageType.fromValue(Integer.MIN_VALUE));
    }

    @Test
    void testMessageTypeValues_ArePositive() {
        for (MessageType type : MessageType.values()) {
            assertTrue(type.getValue() >= 0, 
                    "Message type " + type + " should have non-negative value");
        }
    }
}
