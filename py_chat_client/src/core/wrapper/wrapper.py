import ctypes

ClientContext = ctypes.c_void_p

ON_MESSAGE_CALLBACK = ctypes.CFUNCTYPE(
    None,
    ctypes.c_void_p,
    ctypes.c_int,
    ctypes.POINTER(ctypes.c_uint8),
    ctypes.c_size_t
)

ON_ERROR_CALLBACK = ctypes.CFUNCTYPE(
    None,
    ctypes.c_void_p,
    ctypes.c_int,
    ctypes.c_char_p
)

def config_lib_funcs(lib):
    lib.client_register_complete_message_callback.argtypes = [ClientContext, ON_MESSAGE_CALLBACK, ctypes.c_void_p]
    lib.client_register_complete_message_callback.restype = None

    lib.client_register_error_callback.argtypes = [ClientContext, ON_ERROR_CALLBACK, ctypes.c_void_p]
    lib.client_register_error_callback.restype = None

    lib.client_connect.argtypes = [ctypes.c_char_p, ctypes.c_int]
    lib.client_connect.restype = ClientContext

    lib.client_start_chat.argtypes = [ClientContext]
    lib.client_start_chat.restype = None

    lib.client_shutdown.argtypes = [ClientContext]
    lib.client_shutdown.restype = None

    lib.client_disconnect.argtypes = [ClientContext]
    lib.client_disconnect.restype = None

    lib.client_send_payload.argtypes = [ClientContext, ctypes.c_int, ctypes.POINTER(ctypes.c_uint8), ctypes.c_size_t]
    lib.client_send_payload.restype = None

    lib.client_send_file.argtypes = [ClientContext, ctypes.c_char_p]
    lib.client_send_file.restype = ctypes.c_int

    return lib