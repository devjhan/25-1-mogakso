package project.java_chat_server.service.handlers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.service.FileTransferService;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import java.io.IOException;

@Slf4j
@Component
public class FileChunkHandler extends MessageHandler{
    private final FileTransferService fileTransferService;

    public FileChunkHandler(FileTransferService fileTransferService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.fileTransferService = fileTransferService;
    }

    @Override
    public HandlerResult handle(ClientInfo client, byte[] payload) {
        try {
            if (fileTransferService.getSession(client.socketFd) == null) {
                throw new IllegalStateException("File transfer session not found. FILE_INFO must be sent first.");
            }
            fileTransferService.processFileChunk(client.socketFd, payload);
            return HandlerResult.empty();
        } catch (IllegalStateException e) {
            log.warn("{} : invalid file chunk received from client {}. details: {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("INVALID_SEQUENCE", "파일 정보(FILE_INFO)를 먼저 보내야 합니다.");
        } catch (IOException e) {
            log.error("{} : failed to process file chunk for client {}. details: {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage(), e);
            return super.createErrorResponse("CHUNK_PROCESSING_FAILED", "파일 조각 처리 중 서버 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("{} : unknown error while processing file chunk for client {}. details: {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage(), e);
            return super.createErrorResponse("UNKNOWN_ERROR", "알 수 없는 오류가 발생했습니다.");
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.MSG_TYPE_FILE_CHUNK;
    }
}
