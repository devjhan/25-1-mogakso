package project.java_chat_server.dto.chat;
import lombok.Getter;
import java.time.Instant;

@Getter
public class ChatTextBroadcast {
    private final String author;
    private final String content;
    private final Instant timestamp;

    public ChatTextBroadcast(String author, String content) {
        this.author = author;
        this.content = content;
        this.timestamp = Instant.now();
    }
}
