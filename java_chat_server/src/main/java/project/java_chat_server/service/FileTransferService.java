package project.java_chat_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.java_chat_server.dto.file.FileInfo;
import project.java_chat_server.dto.file.FileTransferState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class FileTransferService {
    private final Map<Integer, FileTransferState> activeTransfers = new ConcurrentHashMap<>();
    private final ObjectMapper jacksonObjectMapper;
    private final String uploadDir;

    public FileTransferService(ObjectMapper jacksonObjectMapper, @Value("${file.upload-dir}") String uploadDir) {
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.uploadDir = uploadDir;
    }

    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("업로드 디렉토리 생성 성공: {}", uploadDir);
            } else {
                log.error("업로드 디렉토리 생성 실패: {}", uploadDir);
            }
        }
    }

    public void startTransfer(int id, String header) {
        FileInfo fileInfo;

        try {
            fileInfo = jacksonObjectMapper.readValue(header, FileInfo.class);
        } catch (JsonProcessingException e) {
            log.error("파일 정보(JSON) 파싱 실패: ID={}, Header={}, Error={}", id, header, e.getMessage());
            return;
        }

        String sanitizedFilename = sanitizeFilename(fileInfo.getFilename());

        if (sanitizedFilename.isEmpty()) {
            log.error("보안 위반: 유효하지 않은 파일 이름 수신: ID={}, Filename={}", id, fileInfo.getFilename());
            return;
        }

        if (activeTransfers.containsKey(id)) {
            log.warn("ID {} 의 클라이언트가 이미 다른 파일을 전송 중입니다. 이전 전송을 취소합니다.", id);
            cancelTransfer(id);
        }

        try {
            String savePath = Paths.get(uploadDir, sanitizedFilename).toString();
            OutputStream os = new FileOutputStream(savePath);

            FileTransferState state = new FileTransferState(sanitizedFilename, savePath, fileInfo.getFilesize(), os);
            activeTransfers.put(id, state);
            log.info("파일 전송 시작: ID={}, 파일 정보={}", id, state);
        } catch (IOException e) {
            log.error("파일 출력 스트림 생성 실패: ID={}, 파일={}", id, sanitizedFilename, e);
        }
    }

    public void processChunk(int id, byte[] chunk) {
        FileTransferState state = activeTransfers.get(id);

        if (state == null) {
            log.warn("파일 전송 시작 정보가 없는 클라이언트(ID:{})로부터 CHUNK를 수신했습니다.", id);
            return;
        }

        try {
            state.getOutputStream().write(chunk);
            state.addReceivedSize(chunk.length);
        } catch (IOException e) {
            log.error("파일 저장 실패 : ID={}", id, e);
            cancelTransfer(id);
            return;
        }

        if (state.getReceivedSize() > state.getFileSize()) {
            log.error("보안 위반: 클라이언트(ID:{})가 명시된 파일 크기보다 많은 데이터를 보냈습니다. 전송을 강제 종료합니다.", id);
            cancelTransfer(id);
        }
    }

    public void cancelTransfer(int id) {
        FileTransferState state = activeTransfers.remove(id);

        if (state == null) {
            return;
        }

        log.warn("ID {} 의 파일 전송을 취소합니다. 파일명: {}", id, state.getFileName());

        try {
            state.getOutputStream().close();
        } catch (IOException e) {
            log.error("취소된 파일 전송의 스트림을 닫는 중 오류 발생: {}, 파일: {}", e.getMessage(), state.getFileName());
        }

        try {
            File partialFile = new File(state.getFullPath());

            if (partialFile.exists()) {
                if (partialFile.delete()) {
                    log.info("부분적으로 업로드된 파일 삭제 성공: {}", state.getFullPath());
                } else {
                    // 파일이 사용 중이거나 권한 문제로 삭제에 실패할 수 있습니다.
                    log.error("부분적으로 업로드된 파일 삭제 실패: {}", state.getFullPath());
                }
            }
        } catch (Exception e) {
            // SecurityException 등 예기치 않은 예외에 대비합니다.
            log.error("부분적으로 업로드된 파일을 삭제하는 중 예외 발생: {}, 파일: {}", e.getMessage(), state.getFileName());
        }
    }

    public FileTransferState completeTransfer(int id) {
        FileTransferState state = activeTransfers.remove(id);

        if (state == null) {
            log.warn("파일 전송 시작 정보가 없는 클라이언트(ID:{})로부터 END 메시지를 수신했습니다.", id);
            return null;
        }

        try {
            state.getOutputStream().close();
        } catch (IOException e) {
            log.error("파일 전송 스트림을 닫는 중 오류 발생: ID={}, 파일={}", id, state.getFileName(), e);
            cleanupFailedTransfer(state);
            return null;
        }

        if (state.getReceivedSize() != state.getFileSize()) {
            log.error("파일 크기 불일치 오류: ID={}, 파일={}, 기대 크기={}, 실제 수신 크기={}", id, state.getFileName(), state.getFileSize(), state.getReceivedSize());
            cleanupFailedTransfer(state);
            return null;
        }

        log.info("파일 전송 성공적으로 완료: ID={}, 파일={}", id, state.getFileName());
        return state;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        return Paths.get(filename).getFileName().toString();
    }

    private void cleanupFailedTransfer(FileTransferState state) {
        log.warn("실패한 파일 전송을 정리합니다: {}", state.getFullPath());
        try {
            File fileToDelete = new File(state.getFullPath());
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
        } catch (Exception e) {
            log.error("실패한 파일을 삭제하는 중 예외 발생: {}", e.getMessage());
        }
    }
}
