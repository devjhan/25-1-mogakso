package project.java_chat_server.wrapper_library;

import com.sun.jna.NativeLong;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import project.java_chat_server.wrapper_library.aliases.ServerContext;
import project.java_chat_server.wrapper_library.c_wrapper.ChatServerLibrary;
import project.java_chat_server.wrapper_library.callbacks.ServerOnClientConnectedCallback;
import project.java_chat_server.wrapper_library.callbacks.ServerOnClientDisconnectedCallback;
import project.java_chat_server.wrapper_library.callbacks.ServerOnCompleteMessageReceivedCallback;
import project.java_chat_server.wrapper_library.callbacks.ServerOnErrorCallback;
import project.java_chat_server.wrapper_library.enums.MessageType;
import java.io.IOException;

@Slf4j
@Getter
public class ChatServer implements AutoCloseable {
    private final ChatServerLibrary lib = ChatServerLibrary.INSTANCE;
    private volatile ServerContext context;
    private final int port;
    private final int maxClients;

    public ChatServer(int port, int maxClients) {
        this.port = port;
        this.maxClients = maxClients;
        this.context = lib.serverCreate(port, maxClients);

        if (context == null) {
            throw new RuntimeException("Failed to create native server context.");
        }
    }

    public void setOnConnectListener(ServerOnClientConnectedCallback listener) {
        lib.serverRegisterConnectCallback(this.context, listener, null);
    }

    public void setOnMessageListener(ServerOnCompleteMessageReceivedCallback listener) {
        lib.serverRegisterCompleteMessageCallback(this.context, listener, null);
    }

    public void setOnDisconnectListener(ServerOnClientDisconnectedCallback listener) {
        lib.serverRegisterDisconnectCallback(this.context, listener, null);
    }

    public void setOnErrorListener(ServerOnErrorCallback listener) {
        lib.serverRegisterErrorCallback(this.context, listener, null);
    }

    public void start() {
        int result = lib.serverStart(this.context);

        if (result != 0) {
            throw new RuntimeException("Failed to start server. Error code: " + result);
        }
    }

    @Override
    public void close() {
        if (this.context != null) {
            lib.serverShutdown(this.context);
            lib.serverDestroy(this.context);
            this.context = null;
        }
    }

    public void sendToClient(final int clientId, final MessageType type, final byte[] payload) throws IOException {
        final ServerContext currentContext = this.context;

        if (currentContext == null) {
            throw new IOException("ChatServer context has already been closed.");
        }

        if (clientId < 0) {
            throw new IllegalArgumentException("Client ID cannot be negative.");
        }

        if (type == null) {
            throw new IllegalArgumentException("Message type cannot be null.");
        }

        if (payload == null || payload.length == 0) {
            log.warn("Attempted to send an empty or null payload to client {}. Aborting.", clientId);
            return;
        }

        final int result = lib.serverSendPayloadToClient(currentContext, clientId, type.getValue(), payload, new NativeLong(payload.length));

        if (result != 0) {
            final String errorMessage = String.format("Failed to send payload to client %d. Native function returned error code: %d", clientId, result);
            log.error(errorMessage);
            throw new IOException(errorMessage);
        }
    }

    public void broadcast(final MessageType type, final byte[] payload, final int excludeClientId) throws IOException {
        final ServerContext currentContext = this.context;

        if (currentContext == null) {
            throw new IOException("ChatServer context has already been closed.");
        }

        if (type == null) {
            throw new IllegalArgumentException("Message type cannot be null.");
        }

        if (payload == null || payload.length == 0) {
            log.warn("Attempted to broadcast an empty or null payload. Aborting.");
            return;
        }

        final int result = lib.serverBroadcastMessage(currentContext, type.getValue(), payload, new NativeLong(payload.length), excludeClientId);

        if (result != 0) {
            final String errorMessage = String.format("Failed to broadcast payload. Native function returned error code: %d", result);
            log.error(errorMessage);
            throw new IOException(errorMessage);
        }
    }
}
