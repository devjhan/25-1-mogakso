package project.java_chat_server.c_native;

import com.sun.jna.*;
import project.java_chat_server.c_native.structure.ClientInfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public interface CLibrary extends Library {
    CLibrary INSTANCE = Native.load("chat", CLibrary.class, Loader.OPTIONS);

    class Loader {
        private Loader() {}
        static final Map<String, Object> OPTIONS;
        static {
            // FunctionMapper를 정의합니다.
            FunctionMapper functionMapper = new FunctionMapper() {
                @Override
                public String getFunctionName(NativeLibrary library, Method method) {
                    String methodName = method.getName();
                    // camelCase를 snake_case로 변환하는 규칙
                    return methodName.replaceAll("([A-Z])", "_$1").toLowerCase();
                }
            };

            // JNA 로드 옵션에 우리가 만든 규칙을 추가합니다.
            OPTIONS = new HashMap<>();
            OPTIONS.put(Library.OPTION_FUNCTION_MAPPER, functionMapper);
        }
    }
    //void (*server_on_client_connected_callback)(void* user_data, const client_info_t* client);
    interface ServerOnClientConnectedCallback extends Callback {
        void invoke(Pointer user_data, ClientInfo.ByReference client);
    }

    //void (*server_on_complete_message_received_callback)(void* user_data, const client_info_t* client, const message_type_t msg_type, const uint8_t* payload, const size_t len);
    interface ServerOnCompleteMessageReceivedCallback extends Callback {
        void invoke(Pointer user_data, ClientInfo.ByReference client, int msg_type, Pointer payload, long len);
    }

    //void (*server_on_client_disconnected_callback)(void* user_data, const client_info_t* client);
    interface ServerOnClientDisconnectedCallback extends Callback {
        void invoke(Pointer user_data, ClientInfo.ByReference client);
    }

    //void (*server_on_error_callback)(void* user_data, const int error_code, const char* message);
    interface ServerOnErrorCallback extends Callback {
        void invoke(Pointer user_data, int error_code, String message);
    }

    //void (*on_complete_callback)(void* user_data, const message_type_t msg_type, const uint8_t* payload, const size_t len);
    interface OnParseCompleteCallback extends Callback {
        void invoke(Pointer user_data, int msg_type, Pointer payload, long len);
    }


    //server_context_t* server_create(const int port, const int max_clients);
    Pointer serverCreate(int port, int maxClients);

    //int server_start(server_context_t* stx);
    int serverStart(Pointer serverContext);

    //void server_send_payload_to_client(const int client_fd, const message_type_t msg_type, const uint8_t* payload, const size_t payload_len);
    void serverSendPayloadToClient(int clientFd, int msgType, byte[] payload, long payloadLen);

    //void server_broadcast_message(const server_context_t* stx, const message_type_t msg_type, const uint8_t* payload, const size_t payload_len, const int exclude_fd);
    void serverBroadcastMessage(Pointer serverContext, int msgType, byte[] payload, long payloadLen, int excludeFd);

    //void server_shutdown(server_context_t* stx);
    void serverShutdown(Pointer serverContext);

    //void server_register_connect_callback(server_context_t* stx, const server_on_client_connected_callback callback, void* user_data);
    void serverRegisterConnectCallback(Pointer serverContext, ServerOnClientConnectedCallback callback, Pointer userData);

    //void server_register_complete_message_callback(server_context_t* stx, const server_on_complete_message_received_callback callback, void* user_data);
    void serverRegisterCompleteMessageCallback(Pointer serverContext, ServerOnCompleteMessageReceivedCallback callback, Pointer userData);

    //void server_register_disconnect_callback(server_context_t* stx, const server_on_client_disconnected_callback callback, void* user_data);
    void serverRegisterDisconnectCallback(Pointer serverContext, ServerOnClientDisconnectedCallback callback, Pointer userData);

    //void server_register_error_callback(server_context_t* stx, const server_on_error_callback callback, void* user_data);
    void serverRegisterErrorCallback(Pointer serverContext, ServerOnErrorCallback callback, Pointer userData);

    //void server_register_parse_complete_callback(server_context_t* stx, const on_complete_callback callback, void* user_data);
    void serverRegisterParseCompleteCallback(Pointer serverContext, OnParseCompleteCallback callback, Pointer userData);
}
