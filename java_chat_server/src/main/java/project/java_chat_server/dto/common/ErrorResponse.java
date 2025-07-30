package project.java_chat_server.dto.common;
import lombok.Getter;
import java.time.Instant;

@Getter
public class ErrorResponse {
    private final String errorCode;
    private final String message;
    private final Instant timestamp;

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
