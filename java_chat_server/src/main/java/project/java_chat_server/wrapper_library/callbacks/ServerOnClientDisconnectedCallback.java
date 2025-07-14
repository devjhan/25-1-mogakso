package project.java_chat_server.wrapper_library.callbacks;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import project.java_chat_server.wrapper_library.structure.ClientInfo;

public interface ServerOnClientDisconnectedCallback extends Callback {
    void invoke(Pointer user_data, ClientInfo client);
}
