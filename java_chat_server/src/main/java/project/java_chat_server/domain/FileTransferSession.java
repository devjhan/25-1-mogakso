package project.java_chat_server.domain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.nio.file.Path;

@RequiredArgsConstructor
@Getter
public class FileTransferSession {
    private final String fileName;
    private final long fileSize;
    private final Path filePath;
    private long receivedBytes = 0;

    public void addReceivedBytes(long length) {
        receivedBytes += length;
    }

    public boolean isCompleted() {
        return receivedBytes == fileSize;
    }
}
