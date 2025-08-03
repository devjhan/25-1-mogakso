import platform
import ctypes
import os

def load_native_library():
    lib_name = "libchat"
    lib_ext = ".dylib" if platform.system() == "Darwin" else ".so"

    current_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.join(current_dir, "..", "..", "..", "..")

    lib_dir = os.path.join(root_dir, "resources", "darwin-aarch64" if platform.system() == "Darwin" else "UNDEFINED")
    lib_path = os.path.join(lib_dir, f'{lib_name}{lib_ext}')

    if not os.path.exists(lib_path):
        raise FileNotFoundError(f"라이브러리 파일을 찾을 수 없습니다: {lib_path}\n"
                                "C_chat_lib 프로젝트를 먼저 빌드했는지 확인해주세요.")
    try:
        return ctypes.CDLL(lib_path)
    except OSError as e:
        raise ImportError(f"네이티브 라이브러리 로드에 실패했습니다: {lib_path}\n에러: {e}")
