package project.java_chat_server.service.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import project.java_chat_server.wrapper_library.enums.MessageType;

import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HandlerResult {
    private final OutgoingMessage directResponse;
    private final OutgoingMessage broadcast;

    public static HandlerResult empty() {
        return new HandlerResult(null, null);
    }

    public static HandlerResult response(MessageType type, Object payload) {
        return new HandlerResult(new OutgoingMessage(type, payload), null);
    }

    public static HandlerResult broadcast(MessageType type, Object payload) {
        return new HandlerResult(null, new OutgoingMessage(type, payload));
    }

    public HandlerResult andBroadcast(MessageType type, Object payload) {
        return new HandlerResult(this.directResponse, new OutgoingMessage(type, payload));
    }

    public Optional<OutgoingMessage> getDirectResponse() {
        return Optional.ofNullable(directResponse);
    }

    public Optional<OutgoingMessage> getBroadcast() {
        return Optional.ofNullable(broadcast);
    }

    public record OutgoingMessage(MessageType type, Object payload) {}
}
