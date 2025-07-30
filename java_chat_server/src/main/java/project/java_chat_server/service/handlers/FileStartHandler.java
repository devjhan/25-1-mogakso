package project.java_chat_server.service.handlers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.java_chat_server.dto.common.SystemNoticeBroadcast;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.dto.file.FileStartBroadcast;
import project.java_chat_server.dto.file.FileStartRequest;
import project.java_chat_server.service.FileTransferService;
import project.java_chat_server.service.UserService;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import java.io.IOException;

@Slf4j
@Component
public class FileStartHandler extends MessageHandler{
    private final FileTransferService fileTransferService;
    private final UserService userService;

    public FileStartHandler(FileTransferService fileTransferService, UserService userService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.fileTransferService = fileTransferService;
        this.userService = userService;
    }

    @Override
    public HandlerResult handle(ClientInfo client, byte[] payload) {
        String senderNickname = null;
        FileStartRequest request = null;

        try {
            senderNickname = userService.getNickname(client.socketFd).orElseThrow(
                    () -> new IllegalStateException("Authentication required. Client not logged in.")
            );

            request = objectMapper.readValue(payload, FileStartRequest.class);
            String filename = request.filename().trim();
            long filesize = request.filesize();

            if (filename.isEmpty() || filesize <= 0) {
                return super.createErrorResponse("INVALID_FILE_INFO", "유효하지 않은 파일 이름 또는 크기입니다.");
            }

            fileTransferService.startFileTransfer(client.socketFd, filename, filesize);

            SystemNoticeBroadcast ackResponse = new SystemNoticeBroadcast("파일 전송을 시작합니다.");
            FileStartBroadcast startBroadcast = new FileStartBroadcast(senderNickname, filename, "STARTED");

            return HandlerResult.response(MessageType.MSG_TYPE_SERVER_NOTICE, ackResponse).andBroadcast(MessageType.MSG_TYPE_FILE_INFO, startBroadcast);
        } catch (IllegalStateException e) {
            log.warn("{} : file transfer rejected for unauthenticated client {}. details : {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("AUTH_REQUIRED", "파일을 전송하려면 먼저 로그인해야 합니다.");
        } catch (IOException e) {
            log.error("{} : failed to parse file info request for client {}. details : {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("INVALID_REQUEST_FORMAT", "요청 형식이 올바르지 않습니다.");
        } catch (Exception e) {
            log.error("{} : failed to start file transfer for client {}. details : {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("FILE_TRANSFER_FAILED", "파일 전송 시작 중 서버 오류가 발생했습니다.");
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.MSG_TYPE_FILE_INFO;
    }
}
