package project.java_chat_server.dto.event_data;

import lombok.Getter;
import project.java_chat_server.common.EventType;

@Getter
public class DisconectEventDataDto extends EventDataDto{
    private final int id;
    public DisconectEventDataDto(EventType type, int id) {
        super(type);
        this.id = id;
    }
}
