"""
Pytest configuration and fixtures
"""
import pytest
import sys
import os
from pathlib import Path
from unittest.mock import MagicMock, patch
import ctypes

# Add project root and src directory to Python path
project_root = Path(__file__).parent.parent
src_path = project_root / "src"
sys.path.insert(0, str(project_root))
sys.path.insert(0, str(src_path))

# Set PYTHONPATH environment variable
os.environ.setdefault('PYTHONPATH', f"{project_root}:{src_path}")


# Create mock at module level - this happens before pytest_configure
_mock_client_lib = MagicMock()
_mock_context = ctypes.c_void_p(12345)
_mock_client_lib.client_connect.return_value = _mock_context
_mock_client_lib.client_register_complete_message_callback = MagicMock()
_mock_client_lib.client_register_error_callback = MagicMock()
_mock_client_lib.client_send_payload = MagicMock()
_mock_client_lib.client_start_chat = MagicMock()
_mock_client_lib.client_shutdown = MagicMock()
_mock_client_lib.client_disconnect = MagicMock()
_mock_client_lib.client_send_file = MagicMock(return_value=0)

# CRITICAL: Patch at module level BEFORE any imports happen
# This must be done before conftest.py is fully loaded
_patcher = None
try:
    _patcher = patch('src.core.wrapper.loader.lib_loader.load_native_library', return_value=_mock_client_lib)
    _patcher.start()
except Exception:
    pass

def pytest_configure(config):
    """Called after command line options have been parsed and all plugins and initial conftest files been loaded"""
    global _mock_client_lib, _mock_context, _patcher
    
    # Ensure patch is active
    if _patcher is None:
        try:
            _patcher = patch('src.core.wrapper.loader.lib_loader.load_native_library', return_value=_mock_client_lib)
            _patcher.start()
        except Exception:
            pass
    
    # If wrapper module is already imported, patch it directly
    if 'src.core.wrapper' in sys.modules:
        import src.core.wrapper as wrapper_module
        wrapper_module.client_lib = _mock_client_lib
        wrapper_module._raw_lib = _mock_client_lib
    
    # Configure mock using wrapper.config_lib_funcs
    try:
        from src.core.wrapper.wrapper import config_lib_funcs
        config_lib_funcs(_mock_client_lib)
    except Exception:
        pass

@pytest.fixture(autouse=True)
def ensure_mock_client_lib(monkeypatch):
    """Ensure mock is applied - autouse fixture"""
    global _mock_client_lib, _mock_context
    
    if _mock_client_lib is None:
        # Create if not already created
        _mock_client_lib = MagicMock()
        _mock_context = ctypes.c_void_p(12345)
        _mock_client_lib.client_connect.return_value = _mock_context
        _mock_client_lib.client_register_complete_message_callback = MagicMock()
        _mock_client_lib.client_register_error_callback = MagicMock()
        _mock_client_lib.client_send_payload = MagicMock()
        _mock_client_lib.client_start_chat = MagicMock()
        _mock_client_lib.client_shutdown = MagicMock()
        _mock_client_lib.client_disconnect = MagicMock()
        _mock_client_lib.client_send_file = MagicMock(return_value=0)
    
    import sys
    import importlib
    
    # Patch load_native_library
    monkeypatch.setattr("src.core.wrapper.loader.lib_loader.load_native_library", lambda: _mock_client_lib)
    
    # If wrapper module is loaded, patch and reload
    if 'src.core.wrapper' in sys.modules:
        import src.core.wrapper as wrapper_module
        
        # Remove from cache
        modules_to_clear = [
            'src.core.wrapper',
            'src.core.chat_client',
            'src.core'
        ]
        for mod_name in modules_to_clear:
            if mod_name in sys.modules:
                del sys.modules[mod_name]
        
        # Reload wrapper module
        try:
            wrapper_module = importlib.import_module('src.core.wrapper')
            # Ensure client_lib is mocked
            monkeypatch.setattr(wrapper_module, "client_lib", _mock_client_lib)
            monkeypatch.setattr(wrapper_module, "_raw_lib", _mock_client_lib)
        except Exception:
            pass
    
    # Ensure mock returns valid context
    _mock_client_lib.client_connect.return_value = _mock_context
    
    return _mock_client_lib

@pytest.fixture
def mock_client_lib(monkeypatch):
    """Provide mock client_lib for tests"""
    global _mock_client_lib
    return _mock_client_lib or ensure_mock_client_lib(monkeypatch)

@pytest.fixture
def temp_file(tmp_path):
    """Create a temporary file for testing"""
    test_file = tmp_path / "test_file.txt"
    test_file.write_text("Hello, World!")
    return str(test_file)

@pytest.fixture
def temp_dir(tmp_path):
    """Create a temporary directory for testing"""
    return str(tmp_path)
