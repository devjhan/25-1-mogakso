package project.java_chat_server.dto.event_data;

import lombok.Getter;
import project.java_chat_server.common.EventType;

@Getter
public class ErrorEventDataDto extends EventDataDto {
    private final int code;
    private final String message;
    
    public ErrorEventDataDto(EventType type,int code, String message) {
        super(type);
        this.code = code;
        this.message = message;
    }
}
