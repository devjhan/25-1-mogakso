package project.java_chat_server.dto.event_data;

import lombok.Getter;
import project.java_chat_server.common.EventType;
import project.java_chat_server.common.MessageType;

@Getter
public class MessageEventDataDto extends EventDataDto {
    private final int id;
    private MessageType msgType;
    private byte[] content;
    
    public MessageEventDataDto(EventType type, int id, MessageType msgType, byte[] content) {
        super(type);
        this.id = id;
        this.msgType = msgType;
        this.content = content;
    }
}
