package project.java_chat_server.wrapper_library.callbacks;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface ServerOnErrorCallback extends Callback {
    void invoke(Pointer user_data, int error_code, String message);
}
