package project.java_chat_server.service.model;

import org.junit.jupiter.api.Test;
import project.java_chat_server.wrapper_library.enums.MessageType;

import static org.junit.jupiter.api.Assertions.*;

class HandlerResultTest {

    @Test
    void testEmpty() {
        HandlerResult result = HandlerResult.empty();

        assertFalse(result.getDirectResponse().isPresent());
        assertFalse(result.getBroadcast().isPresent());
    }

    @Test
    void testResponse() {
        Object payload = new Object();
        HandlerResult result = HandlerResult.response(MessageType.MSG_TYPE_CHAT_TEXT, payload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_CHAT_TEXT, result.getDirectResponse().get().type());
        assertEquals(payload, result.getDirectResponse().get().payload());
        assertFalse(result.getBroadcast().isPresent());
    }

    @Test
    void testBroadcast() {
        Object payload = new Object();
        HandlerResult result = HandlerResult.broadcast(MessageType.MSG_TYPE_USER_JOIN_NOTICE, payload);

        assertFalse(result.getDirectResponse().isPresent());
        assertTrue(result.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_USER_JOIN_NOTICE, result.getBroadcast().get().type());
        assertEquals(payload, result.getBroadcast().get().payload());
    }

    @Test
    void testAndBroadcast() {
        Object responsePayload = new Object();
        Object broadcastPayload = new Object();

        HandlerResult result = HandlerResult.response(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, responsePayload)
                .andBroadcast(MessageType.MSG_TYPE_USER_JOIN_NOTICE, broadcastPayload);

        assertTrue(result.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, result.getDirectResponse().get().type());
        assertEquals(responsePayload, result.getDirectResponse().get().payload());

        assertTrue(result.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_USER_JOIN_NOTICE, result.getBroadcast().get().type());
        assertEquals(broadcastPayload, result.getBroadcast().get().payload());
    }

    @Test
    void testAndBroadcast_Chaining() {
        Object payload1 = new Object();
        Object payload2 = new Object();
        Object payload3 = new Object();

        HandlerResult result = HandlerResult.response(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, payload1)
                .andBroadcast(MessageType.MSG_TYPE_USER_JOIN_NOTICE, payload2);

        // Second andBroadcast should replace the previous broadcast
        HandlerResult result2 = result.andBroadcast(MessageType.MSG_TYPE_USER_LEAVE_NOTICE, payload3);

        assertTrue(result2.getDirectResponse().isPresent());
        assertEquals(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, result2.getDirectResponse().get().type());
        assertTrue(result2.getBroadcast().isPresent());
        assertEquals(MessageType.MSG_TYPE_USER_LEAVE_NOTICE, result2.getBroadcast().get().type());
        assertEquals(payload3, result2.getBroadcast().get().payload());
    }

    @Test
    void testAllMessageTypes() {
        for (MessageType type : MessageType.values()) {
            if (type != MessageType.MSG_TYPE_UNKNOWN) {
                Object payload = new Object();
                HandlerResult result = HandlerResult.response(type, payload);

                assertTrue(result.getDirectResponse().isPresent());
                assertEquals(type, result.getDirectResponse().get().type());
            }
        }
    }

    @Test
    void testNullPayload_Response() {
        HandlerResult result = HandlerResult.response(MessageType.MSG_TYPE_CHAT_TEXT, null);

        assertTrue(result.getDirectResponse().isPresent());
        assertNull(result.getDirectResponse().get().payload());
    }

    @Test
    void testNullPayload_Broadcast() {
        HandlerResult result = HandlerResult.broadcast(MessageType.MSG_TYPE_USER_JOIN_NOTICE, null);

        assertTrue(result.getBroadcast().isPresent());
        assertNull(result.getBroadcast().get().payload());
    }
}
