package project.java_chat_server.wrapper_library.c_wrapper;
import com.sun.jna.*;
import project.java_chat_server.wrapper_library.callbacks.ServerOnClientConnectedCallback;
import project.java_chat_server.wrapper_library.callbacks.ServerOnClientDisconnectedCallback;
import project.java_chat_server.wrapper_library.callbacks.ServerOnCompleteMessageReceivedCallback;
import project.java_chat_server.wrapper_library.callbacks.ServerOnErrorCallback;
import project.java_chat_server.wrapper_library.aliases.ServerContext;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public interface ChatServerLibrary extends Library {
    class CaseChangingFunctionMapper implements FunctionMapper {
        @Override
        public String getFunctionName(NativeLibrary library, Method method) {
            String methodName = method.getName();
            return methodName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
        }
    }
    String LIBRARY_NAME = "chat";

    Map<String, Object> OPTIONS = new HashMap<>() {{
        put(Library.OPTION_FUNCTION_MAPPER, new CaseChangingFunctionMapper());
    }};

    ChatServerLibrary INSTANCE = Native.load(LIBRARY_NAME, ChatServerLibrary.class, OPTIONS);

    void serverRegisterConnectCallback(ServerContext stx, ServerOnClientConnectedCallback callback, Pointer userData);
    void serverRegisterCompleteMessageCallback(ServerContext stx, ServerOnCompleteMessageReceivedCallback callback, Pointer userData);
    void serverRegisterDisconnectCallback(ServerContext stx, ServerOnClientDisconnectedCallback callback, Pointer userData);
    void serverRegisterErrorCallback(ServerContext stx, ServerOnErrorCallback callback, Pointer userData);

    ServerContext serverCreate(int port, int maxClients);
    void serverShutdown(ServerContext stx);
    void serverDestroy(ServerContext stx);
    int serverStart(ServerContext stx);

    int serverSendPayloadToClient(ServerContext stx, int clientFd, int msgType, byte[] payload, NativeLong payloadLen);
    int serverBroadcastMessage(ServerContext stx, int msgType, byte[] payload, NativeLong payloadLen, int excludeFd);
}
