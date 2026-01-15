"""
Tests for ChatClient class
"""
import pytest
import ctypes
import os
import hashlib
import sys
import importlib
from unittest.mock import MagicMock, patch, mock_open
from src.core.dto import UserLoginRequest, ChatTextRequest, FileStartRequest, FileEndRequest
from src.core.enums import MessageType


@pytest.mark.unit
class TestChatClient:
    """Unit tests for ChatClient"""
    
    
    def test_chat_client_initialization_success(self, ensure_mock_client_lib):
        """Test successful ChatClient initialization"""
        import sys
        import importlib
        
        # Clear and reload chat_client to use mock
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        if 'src.core.wrapper' in sys.modules:
            import src.core.wrapper as wrapper_module
            wrapper_module.client_lib = ensure_mock_client_lib
        
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        assert client._ctx is not None
        assert client.chunk_size == 4096  # Default chunk size
        assert client._on_message_callback is None
        assert client._on_error_callback is None
    
    def test_chat_client_initialization_custom_chunk_size(self, ensure_mock_client_lib):
        """Test ChatClient initialization with custom chunk size"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000, chunk_size=8192)
        assert client.chunk_size == 8192
    
    def test_chat_client_initialization_connection_failure(self, ensure_mock_client_lib, monkeypatch):
        """Test ChatClient initialization with connection failure"""
        ensure_mock_client_lib.client_connect.return_value = None
        
        import sys
        import importlib
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        if 'src.core.wrapper' in sys.modules:
            import src.core.wrapper as wrapper_module
            wrapper_module.client_lib = ensure_mock_client_lib
        
        from src.core.chat_client import ChatClient
        
        with pytest.raises(ConnectionError, match="서버 연결에 실패했습니다"):
            ChatClient("127.0.0.1", 9000)
    
    def test_register_on_message_callback(self, ensure_mock_client_lib):
        """Test registering message callback"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        callback = MagicMock()
        
        client.register_on_message_callback(callback)
        
        assert client._on_message_callback == callback
        ensure_mock_client_lib.client_register_complete_message_callback.assert_called_once()
    
    def test_register_on_error_callback(self, ensure_mock_client_lib):
        """Test registering error callback"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        callback = MagicMock()
        
        client.register_on_error_callback(callback)
        
        assert client._on_error_callback == callback
        ensure_mock_client_lib.client_register_error_callback.assert_called_once()
    
    def test_send_login_request(self, ensure_mock_client_lib):
        """Test sending login request"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        request = UserLoginRequest(nickname="testuser")
        
        client.send_login_request(request)
        
        ensure_mock_client_lib.client_send_payload.assert_called_once()
        call_args = ensure_mock_client_lib.client_send_payload.call_args
        assert call_args[0][1] == MessageType.MSG_TYPE_USER_LOGIN_REQUEST.value
    
    def test_send_chat_text(self, ensure_mock_client_lib):
        """Test sending chat text"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        # Reset mock call count
        ensure_mock_client_lib.client_send_payload.reset_mock()
        
        client = ChatClient("127.0.0.1", 9000)
        request = ChatTextRequest(message="Hello, World!")
        
        client.send_chat_text(request)
        
        # Should be called once for the chat text message
        assert ensure_mock_client_lib.client_send_payload.call_count >= 1
        # Check the last call was with CHAT_TEXT message type
        last_call = ensure_mock_client_lib.client_send_payload.call_args_list[-1]
        assert last_call[0][1] == MessageType.MSG_TYPE_CHAT_TEXT.value
    
    def test_send_file_success(self, ensure_mock_client_lib, temp_file):
        """Test sending file successfully"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        
        with patch('builtins.open', mock_open(read_data=b"Hello, World!")):
            with patch('os.path.getsize', return_value=13):
                client.send_file(temp_file)
        
        # Verify file start, chunks, and file end were sent
        assert ensure_mock_client_lib.client_send_payload.call_count >= 2
    
    def test_send_file_not_found(self, ensure_mock_client_lib):
        """Test sending non-existent file"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        
        with pytest.raises(FileNotFoundError, match="파일을 찾을 수 없습니다"):
            client.send_file("/nonexistent/file.txt")
    
    def test_send_file_directory(self, ensure_mock_client_lib, temp_dir):
        """Test sending directory instead of file"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        
        with pytest.raises(IsADirectoryError, match="경로는 파일이 아닌 디렉토리입니다"):
            client.send_file(temp_dir)
    
    def test_send_payload_with_disconnected_client(self, ensure_mock_client_lib):
        """Test sending payload when client is disconnected"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        client.disconnect()
        
        with pytest.raises(ConnectionError, match="클라이언트가 이미 종료되었습니다"):
            client._send_payload(MessageType.MSG_TYPE_CHAT_TEXT, b"test")
    
    def test_start_chat_with_connected_client(self, ensure_mock_client_lib):
        """Test starting chat with connected client"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        
        # This would block in real implementation, but with mock it should just call
        client.start_chat()
        
        ensure_mock_client_lib.client_start_chat.assert_called_once()
    
    def test_start_chat_with_disconnected_client(self, ensure_mock_client_lib):
        """Test starting chat when client is disconnected"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        client.disconnect()
        
        with pytest.raises(ConnectionError, match="클라이언트가 이미 종료되었습니다"):
            client.start_chat()
    
    def test_shutdown_with_connected_client(self, ensure_mock_client_lib):
        """Test shutting down connected client"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        
        client.shutdown()
        
        ensure_mock_client_lib.client_shutdown.assert_called_once()
    
    def test_shutdown_with_disconnected_client(self, ensure_mock_client_lib):
        """Test shutting down disconnected client (should not raise error)"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        client.disconnect()
        
        # Should not raise error
        client.shutdown()
    
    def test_disconnect(self, ensure_mock_client_lib):
        """Test disconnecting client"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        # Reset mock
        ensure_mock_client_lib.client_disconnect.reset_mock()
        
        client = ChatClient("127.0.0.1", 9000)
        
        client.disconnect()
        
        assert client._ctx is None
        assert ensure_mock_client_lib.client_disconnect.call_count >= 1
    
    def test_context_manager(self, ensure_mock_client_lib):
        """Test ChatClient as context manager"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        # Reset mock
        ensure_mock_client_lib.client_disconnect.reset_mock()
        
        with ChatClient("127.0.0.1", 9000) as client:
            assert client._ctx is not None
        
        # After context exit, client should be disconnected
        assert client._ctx is None
        assert ensure_mock_client_lib.client_disconnect.call_count >= 1


@pytest.mark.unit
class TestChatClientFileTransfer:
    """Edge case tests for ChatClient file transfer"""
    
    def test_send_file_empty_file(self, ensure_mock_client_lib, tmp_path):
        """Test sending empty file"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        empty_file = tmp_path / "empty.txt"
        empty_file.write_bytes(b"")
        
        with patch('builtins.open', mock_open(read_data=b"")):
            with patch('os.path.getsize', return_value=0):
                client.send_file(str(empty_file))
        
        # Should still send file start and end
        assert ensure_mock_client_lib.client_send_payload.call_count >= 2
    
    def test_send_file_large_file(self, ensure_mock_client_lib, tmp_path):
        """Test sending large file (multiple chunks)"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000, chunk_size=100)
        large_file = tmp_path / "large.txt"
        large_file.write_bytes(b"x" * 1000)  # Create actual file
        
        # Create large file data (1000 bytes, will be split into 10 chunks)
        large_data = b"x" * 1000
        
        # Reset mock
        ensure_mock_client_lib.client_send_payload.reset_mock()
        
        with patch('builtins.open', mock_open(read_data=large_data)):
            with patch('os.path.getsize', return_value=1000):
                with patch('os.path.exists', return_value=True):
                    with patch('os.path.isfile', return_value=True):
                        client.send_file(str(large_file))
        
        # Should send file start, multiple chunks, and file end
        assert ensure_mock_client_lib.client_send_payload.call_count >= 12  # 1 start + 10 chunks + 1 end
    
    def test_send_file_permission_error(self, ensure_mock_client_lib, tmp_path):
        """Test sending file with permission error"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        test_file = tmp_path / "test.txt"
        test_file.write_text("test")
        
        with patch('builtins.open', side_effect=PermissionError("Permission denied")):
            with pytest.raises(RuntimeError, match="파일의 권한에 관련된 오류"):
                client.send_file(str(test_file))
    
    def test_send_file_io_error(self, ensure_mock_client_lib, tmp_path):
        """Test sending file with IO error"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        test_file = tmp_path / "test.txt"
        test_file.write_text("test")
        
        with patch('builtins.open', side_effect=IOError("IO error")):
            with pytest.raises(RuntimeError, match="파일을 읽는 중 오류"):
                client.send_file(str(test_file))
    
    def test_send_file_connection_error(self, ensure_mock_client_lib, tmp_path):
        """Test sending file with connection error"""
        import sys
        if 'src.core.chat_client' in sys.modules:
            del sys.modules['src.core.chat_client']
        from src.core.chat_client import ChatClient
        
        client = ChatClient("127.0.0.1", 9000)
        test_file = tmp_path / "test.txt"
        test_file.write_text("test")
        
        ensure_mock_client_lib.client_send_payload.side_effect = ConnectionError("Connection lost")
        
        with patch('builtins.open', mock_open(read_data=b"test")):
            with patch('os.path.getsize', return_value=4):
                with pytest.raises(ConnectionError, match="전송 중 연결 오류"):
                    client.send_file(str(test_file))
