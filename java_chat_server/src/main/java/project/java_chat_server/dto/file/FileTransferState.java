package project.java_chat_server.dto.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.OutputStream;

@Getter
@RequiredArgsConstructor
public class FileTransferState {
    private final String fileName;
    private final String fullPath;
    private final long fileSize;
    private final OutputStream outputStream;
    private long receivedSize = 0;

    public void addReceivedSize(long chunkSize) {
        this.receivedSize += chunkSize;
    }
}
