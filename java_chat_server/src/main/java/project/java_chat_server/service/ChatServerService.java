package project.java_chat_server.service;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.java_chat_server.c_native.CLibrary;
import project.java_chat_server.common.EventType;
import project.java_chat_server.dto.UserDto;
import project.java_chat_server.common.MessageType;
import project.java_chat_server.dto.event_data.*;
import project.java_chat_server.dto.file.FileTransferState;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
public class ChatServerService {
    private CLibrary lib;
    private final UserService userService;
    private final FileTransferService fileTransferService;
    private Pointer serverHandle;

    private CLibrary.ServerOnClientConnectedCallback onConnectCallback;
    private CLibrary.ServerOnClientDisconnectedCallback onDisconnectCallback;
    private CLibrary.ServerOnCompleteMessageReceivedCallback onMessageCallback;
    private CLibrary.ServerOnErrorCallback serverOnErrorCallback;

    private final BlockingQueue<EventDataDto> eventQ = new LinkedBlockingQueue<>();

    public ChatServerService(UserService userService, FileTransferService fileTransferService) {
        this.userService = userService;
        this.fileTransferService = fileTransferService;
    }

    @PostConstruct
    public void init() {
        try {
            lib = CLibrary.INSTANCE;
            log.info("네이티브 C 채팅 라이브러리 로드 성공.");

            int port = 8080;
            int maxClients = 100;
            serverHandle = lib.serverCreate(port, maxClients);

            if (serverHandle == null) {
                throw new IllegalStateException("C 서버 컨텍스트 생성에 실패했습니다.");
            }
            log.info("C 서버 인스턴스 생성 성공 (Port: {}, Max Clients: {})", port, maxClients);

            initAndRegisterCallbacks();
            log.info("Java 콜백 함수가 C 라이브러리에 성공적으로 등록되었습니다.");

            lib.serverStart(serverHandle);
            log.info("C 서버가 백그라운드 스레드에서 시작되었습니다.");

            startEventQProcessor();
        } catch (UnsatisfiedLinkError e) {
            log.error("네이티브 라이브러리 로드에 실패했습니다. JVM 실행 옵션에서 'java.library.path'가 올바르게 설정되었는지 확인해주세요.", e);
            System.exit(1); // 애플리케이션 정상 실행 불가
        } catch (Exception e) {
            log.error("서버 시작 중 치명적인 오류가 발생했습니다.", e);
            System.exit(1);
        }
    }

    @PreDestroy
    public void destroy() {
        if (serverHandle != null && lib != null) {
            log.info("C 서버 종료를 시작합니다...");
            lib.serverShutdown(serverHandle);
            log.info("C 서버가 안전하게 종료되었습니다.");
        }
    }

    private void initAndRegisterCallbacks() {
        this.onConnectCallback = (userData, client) -> eventQ.offer(new ConnectEventDataDto(EventType.EVENT_TYPE_CONNECT, client.socketFd, Native.toString(client.ipAddr)));
        this.onDisconnectCallback = (userData, client) -> eventQ.offer(new DisconectEventDataDto(EventType.EVENT_TYPE_DISCONNECT, client.socketFd));
        this.onMessageCallback = (userData, client, msgType, payload, len) -> eventQ.offer(new MessageEventDataDto(EventType.EVENT_TYPE_MESSAGE, client.socketFd, MessageType.fromValue(msgType), payload.getByteArray(0, (int) len)));
        this.serverOnErrorCallback = (userData, errorCode, message) -> eventQ.offer(new ErrorEventDataDto(EventType.EVENT_TYPE_ERROR, errorCode, message));

        lib.serverRegisterConnectCallback(serverHandle, this.onConnectCallback, null);
        lib.serverRegisterDisconnectCallback(serverHandle, this.onDisconnectCallback, null);
        lib.serverRegisterCompleteMessageCallback(serverHandle, this.onMessageCallback, null);
        lib.serverRegisterErrorCallback(serverHandle, this.serverOnErrorCallback, null);
    }

    private void startEventQProcessor() {
        Thread processorThread = new Thread(() -> {
            log.info("이벤트 큐 처리 스레드를 시작합니다.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    EventDataDto event = eventQ.take();
                    log.info("[Event-Processor] 이벤트 처리: {}", event);
                    processEvent(event.getType(), event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("이벤트 큐 처리 스레드가 중단되었습니다.");
                }
            }
        });
        processorThread.setName("C-Event-Queue-Processor");
        processorThread.setDaemon(true); // 주 애플리케이션 종료 시 함께 종료되도록 설정
        processorThread.start();
    }

    private void processEvent(EventType type, EventDataDto event) {
        switch (type) {
            case EVENT_TYPE_CONNECT -> {
                ConnectEventDataDto connectEvent = (ConnectEventDataDto) event;
                log.info("새로운 클라이언트 연결 감지: ID={}, IP={}. 로그인 대기 상태로 전환합니다.", connectEvent.getId(), connectEvent.getIp());
                userService.handleNewConnection(connectEvent.getId(), connectEvent.getIp());
            }
            case EVENT_TYPE_DISCONNECT -> {
                DisconectEventDataDto disconnectEvent = (DisconectEventDataDto) event;
                int clientId = disconnectEvent.getId();

                log.info("ID {} 의 연결 해제에 따른 파일 전송 리소스를 정리합니다.", clientId);
                fileTransferService.cancelTransfer(clientId);

                final UserDto disconnectedUser = userService.removeOnlineUser(disconnectEvent.getId());

                if (disconnectedUser == null) {
                    log.warn("이미 제거되었거나 존재하지 않는 사용자(ID:{})에 대한 연결 해제 이벤트를 수신했습니다.", clientId);
                    break;
                }

                if (disconnectedUser.getState() == UserDto.UserState.CONNECTED) {
                    log.info("미인증 사용자(ID:{})의 연결이 해제되었습니다.", clientId);
                    break;
                }
                String leaveNotice = String.format("[System] %s 님이 퇴장했습니다.", disconnectedUser.getUsername());
                log.info("퇴장 알림 전송: {}", leaveNotice);
                broadcastMessage(leaveNotice, MessageType.MSG_TYPE_USER_LEAVE_NOTICE,-1);
            }
            case EVENT_TYPE_MESSAGE -> {
                MessageEventDataDto messageEvent = (MessageEventDataDto) event;
                processMessageByMsgType(messageEvent.getId(), messageEvent.getMsgType(), messageEvent.getContent());
            }
            case EVENT_TYPE_ERROR -> {
                ErrorEventDataDto errorEvent = (ErrorEventDataDto) event;
                log.error("C 라이브러리에서 심각한 오류가 감지되었습니다. Code: {}, Message: '{}'", errorEvent.getCode(), errorEvent.getMessage());
            }
            default -> {
                log.error("처리할 수 없는 알 수 없는 이벤트 타입 수신: {}", event);
            }
        }
    }

    private void processMessageByMsgType(int id, MessageType msgType, byte[] content) {
        switch (msgType) {
            case MSG_TYPE_CHAT_TEXT -> handleChatMessage(id, content);
            case MSG_TYPE_FILE_INFO -> fileTransferService.startTransfer(id, new String(content, StandardCharsets.UTF_8));
            case MSG_TYPE_FILE_CHUNK -> fileTransferService.processChunk(id, content);
            case MSG_TYPE_FILE_END -> handleCompleteFileTransfer(id);
            case MSG_TYPE_USER_LOGIN_REQUEST -> handleLoginRequest(id, content);
            case MSG_TYPE_USER_LOGIN_RESPONSE, MSG_TYPE_USER_JOIN_NOTICE, MSG_TYPE_USER_LEAVE_NOTICE,
                 MSG_TYPE_SERVER_NOTICE, MSG_TYPE_ERROR_RESPONSE, MSG_TYPE_PING, MSG_TYPE_PONG -> handleInvalidAccess(id, msgType);
            default -> handleDefault(id, msgType);
        }
    }

    private void handleCompleteFileTransfer(int id) {
        FileTransferState completeState = fileTransferService.completeTransfer(id);

        if (completeState == null) {
            String failNotice = "[SYSTEM] 파일 전송이 실패했습니다.";
            sendMessageToClient(failNotice, MessageType.MSG_TYPE_SERVER_NOTICE, id);
            return;
        }
        UserDto user = userService.getOnlineUser(id);
        String username = user!= null ? user.getUsername() : "알 수 없는 사용자";
        String sentNotice = String.format("[SYSTEM] %s 님으로부터 %s 가(이) 성공적으로 전송되었습니다.", username, completeState.getFileName());
        broadcastMessage(sentNotice, MessageType.MSG_TYPE_SERVER_NOTICE, id);
        sendMessageToClient("[SYSTEM] 파일 전송이 성공적으로 완료되었습니다.", MessageType.MSG_TYPE_SERVER_NOTICE, id);
    }

    private void handleLoginRequest(int id, byte[] content) {
        String username = new String(content, StandardCharsets.UTF_8);
        log.info("로그인 요청 수신: ID={}, Username={}", id, username);
        boolean success = userService.authenticateUser(id, username);

        if (success) {
            sendMessageToClient("로그인에 성공했습니다. 채팅을 시작하세요.", MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, id);
            String joinNotice = String.format("[System] %s 님이 입장했습니다.", username);
            broadcastMessage(joinNotice, MessageType.MSG_TYPE_USER_JOIN_NOTICE, id);
        } else {
            log.warn("로그인 실패: ID={}, Username={}. 이미 인증되었거나 잘못된 접근입니다.", id, username);
            sendMessageToClient("로그인에 실패했습니다. (사유: 닉네임 중복 또는 잘못된 접근)", MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, id);
        }
    }

    private void handleChatMessage(int id, byte[] content) {
        UserDto user = userService.getOnlineUser(id);

        if (user == null || user.getState() == UserDto.UserState.CONNECTED) {
            log.warn("인증되지 않은 사용자(ID:{})의 채팅 메시지 전송 시도를 차단했습니다.", id);
            sendMessageToClient("오류: 로그인이 필요합니다.", MessageType.MSG_TYPE_ERROR_RESPONSE, id);
            return;
        }
        String fullMessage = String.format("[%s] %s", user.getUsername(), new String(content, StandardCharsets.UTF_8));
        log.info("채팅 메시지 중계: {}", fullMessage);
        broadcastMessage(fullMessage, MessageType.MSG_TYPE_CHAT_TEXT, id);
    }

    private void handleInvalidAccess(int id, MessageType msgType) {
        log.warn("보안 경고: 클라이언트(ID:{})로부터 서버 전용 메시지 타입({})을 수신했습니다. 클라이언트 변조가 의심됩니다.", id, msgType);

    }

    private void handleDefault(int id, MessageType msgType) {
        log.error("알 수 없는 메시지 타입 수신: ID={}, RawType={}", id, msgType);
        sendMessageToClient("지원하지 않는 요청입니다 (CODE: " + msgType + ")", MessageType.MSG_TYPE_ERROR_RESPONSE, id);
    }

    public void broadcastMessage(String message, MessageType msgType, int excludeClientFd) {
        if (serverHandle == null) {
            log.warn("C 서버가 초기화되지 않아 브로드캐스트를 실행할 수 없습니다.");
            return;
        }

        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        lib.serverBroadcastMessage(serverHandle, msgType.getValue(), payload, payload.length, excludeClientFd);
    }

    public void sendMessageToClient(String message, MessageType msgType, int clientFd) {
        if (serverHandle == null) {
            log.warn("C 서버가 초기화되지 않아 개인 메시지를 보낼 수 없습니다.");
            return;
        }

        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        lib.serverSendPayloadToClient(clientFd, msgType.getValue(), payload, payload.length);
    }
}
