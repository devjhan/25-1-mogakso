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
