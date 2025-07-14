package project.java_chat_server.wrapper_library.callbacks;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import project.java_chat_server.wrapper_library.structure.ClientInfo;

public interface ServerOnClientConnectedCallback extends Callback {
    void invoke(Pointer userData, ClientInfo client);
}
