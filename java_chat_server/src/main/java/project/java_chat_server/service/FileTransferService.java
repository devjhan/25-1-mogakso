package project.java_chat_server.service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.commons.codec.digest.DigestUtils;
import project.java_chat_server.domain.FileTransferSession;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FileTransferService {
    private final Map<Integer, FileTransferSession> activeSessions = new ConcurrentHashMap<>();
    private final Path uploadDirectory;

    public FileTransferService(@Value("${file.upload-directory}") String uploadDirectory) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.uploadDirectory);
            log.info("파일 업로드 디렉토리 초기화 완료: {}", this.uploadDirectory);
        } catch (IOException e) {
            log.error("치명적 오류: 파일 업로드 디렉토리({})를 생성할 수 없습니다.", this.uploadDirectory, e);
            throw new RuntimeException("파일 업로드 디렉토리 생성에 실패했습니다.", e);
        }
    }


    public void startFileTransfer(int clientId, String fileName, long fileSize) throws IOException {
        if (activeSessions.containsKey(clientId)) {
            throw new IOException("이미 진행 중인 파일 전송이 있습니다.");
        }

        Path filePath = uploadDirectory.resolve(System.currentTimeMillis() + "_" + fileName);
        FileTransferSession session = new FileTransferSession(fileName, fileSize, filePath);

        activeSessions.put(clientId, session);
        log.info("파일 전송 시작: 클라이언트(id:{}) -> 파일 '{}' ({} bytes)", clientId, fileName, fileSize);
    }

    public void processFileChunk(int clientId, byte[] chunk) throws IOException {
        FileTransferSession session = activeSessions.get(clientId);

        if (session == null) {
            throw new IOException("해당 클라이언트의 파일 전송 세션이 존재하지 않습니다.");
        }

        Files.write(session.getFilePath(), chunk, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        session.addReceivedBytes(chunk.length);
    }

    public FileTransferSession endFileTransfer(int clientId, String clientChecksum) throws IOException {
        FileTransferSession session = activeSessions.get(clientId);

        if (session == null) {
            throw new IOException("해당 클라이언트의 파일 전송 세션이 존재하지 않습니다.");
        }

        if (!session.isCompleted()) {
            activeSessions.remove(clientId);
            try {
                Files.deleteIfExists(session.getFilePath());
            } catch (IOException e) {
                log.error("불완전한 전송 파일 삭제 실패: {}", session.getFilePath(), e);
            }
            throw new IOException(String.format("파일 전송이 불완전합니다. 예상 크기: %d, 수신 크기: %d", session.getFileSize(), session.getReceivedBytes()));
        }

        String serverChecksum;

        try (InputStream is = Files.newInputStream(session.getFilePath())) {
            serverChecksum = DigestUtils.sha256Hex(is);
        }

        if (!serverChecksum.equals(clientChecksum)) {
            Files.delete(session.getFilePath());
            activeSessions.remove(clientId);
            throw new IOException("파일 무결성 검증 실패: 체크섬이 일치하지 않습니다.");
        }

        activeSessions.remove(clientId);
        return session;
    }

    public FileTransferSession getSession(int clientId) {
        return activeSessions.get(clientId);
    }

    public void cancelFileTransfer(int clientId, String reason) {
        FileTransferSession session = activeSessions.remove(clientId);

        if (session != null) {
            try {
                Files.deleteIfExists(session.getFilePath());
            } catch (IOException e) {
                log.error("취소된 전송의 임시 파일 삭제 실패", e);
            }
            log.warn("파일 전송이 취소되었습니다: id={}, 파일={}, 사유: {}", clientId, session.getFileName(), reason);
        }
    }

}
