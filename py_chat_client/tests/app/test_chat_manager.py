"""
Tests for ChatManager (with mocks)
"""
import pytest
from unittest.mock import MagicMock, patch
from src.app.chat_manager import ChatManager
from src.app.events import EventManager, EventType
from src.configs.configuration import Configuration
from src.core.dto import UserLoginResponse, ChatTextBroadcast, ErrorResponse
from src.core.enums import MessageType


@pytest.fixture
def mock_config():
    """Create a mock Configuration"""
    config = MagicMock(spec=Configuration)
    config.SERVER_IP = "127.0.0.1"
    config.SERVER_PORT = 9000
    config.CHUNK_SIZE = 4096
    return config


@pytest.fixture
def event_manager():
    """Create an EventManager instance"""
    return EventManager()


@pytest.mark.unit
class TestChatManager:
    """Unit tests for ChatManager"""
    
    def test_chat_manager_initialization(self, mock_config, event_manager):
        """Test ChatManager initialization"""
        manager = ChatManager(mock_config, event_manager)
        
        assert manager.config == mock_config
        assert manager.event_manager == event_manager
        assert manager.nickname is None
        assert manager.client_id is None
        assert manager._client is None
        assert manager._chat_thread is None
        assert not manager.is_logged_in()
        assert not manager.is_connected()
    
    def test_is_logged_in_false(self, mock_config, event_manager):
        """Test is_logged_in when not logged in"""
        manager = ChatManager(mock_config, event_manager)
        assert manager.is_logged_in() is False
    
    def test_is_logged_in_true(self, mock_config, event_manager):
        """Test is_logged_in when logged in"""
        manager = ChatManager(mock_config, event_manager)
        manager.nickname = "testuser"
        assert manager.is_logged_in() is True
    
    def test_is_connected_false(self, mock_config, event_manager):
        """Test is_connected when not connected"""
        manager = ChatManager(mock_config, event_manager)
        assert manager.is_connected() is False
    
    @patch('src.app.chat_manager.ChatClient')
    def test_is_connected_true(self, mock_client_class, mock_config, event_manager):
        """Test is_connected when connected"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        assert manager.is_connected() is True
    
    @patch('src.app.chat_manager.ChatClient')
    def test_is_connected_true(self, mock_client_class, mock_config, event_manager):
        """Test is_connected when connected"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        assert manager.is_connected() is True
    
    @patch('src.app.chat_manager.ChatClient')
    def test_connect_success(self, mock_client_class, mock_config, event_manager):
        """Test successful connection"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        
        assert manager._client is not None
        assert manager.is_connected() is True
        mock_client_class.assert_called_once()
    
    @patch('src.app.chat_manager.ChatClient')
    def test_connect_already_connected(self, mock_client_class, mock_config, event_manager):
        """Test connecting when already connected"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        initial_client = manager._client
        
        manager.connect()  # Try to connect again
        
        # Should not create new client (only called once)
        assert manager._client == initial_client
        assert mock_client_class.call_count == 1
    
    @patch('src.app.chat_manager.ChatClient')
    def test_connect_failure(self, mock_client_class, mock_config, event_manager):
        """Test connection failure"""
        mock_client_class.side_effect = ConnectionError("Connection failed")
        
        manager = ChatManager(mock_config, event_manager)
        
        # Should fire CONNECTION_FAILURE event, not raise exception
        event_fired = []
        def handler(data):
            event_fired.append(data)
        
        event_manager.subscribe(EventType.CONNECTION_FAILURE, handler)
        manager.connect()
        
        # Should fire failure event
        assert len(event_fired) == 1
        assert manager._client is None
        assert not manager.is_connected()
    
    def test_disconnect_when_not_connected(self, mock_config, event_manager):
        """Test disconnecting when not connected"""
        manager = ChatManager(mock_config, event_manager)
        
        # Should not raise error
        manager.disconnect()
    
    @patch('src.app.chat_manager.ChatClient')
    def test_disconnect_when_connected(self, mock_client_class, mock_config, event_manager):
        """Test disconnecting when connected"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        manager.disconnect()
        
        assert manager._client is None
        assert not manager.is_connected()
        mock_client.shutdown.assert_called_once()
        mock_client.disconnect.assert_called_once()
    
    def test_try_login_when_not_connected(self, mock_config, event_manager):
        """Test try_login when not connected"""
        manager = ChatManager(mock_config, event_manager)
        event_fired = []
        
        def handler(data):
            event_fired.append(data)
        
        event_manager.subscribe(EventType.LOGIN_FAILURE, handler)
        manager.try_login("testuser")
        
        assert len(event_fired) == 1
    
    @patch('src.app.chat_manager.ChatClient')
    def test_try_login_success(self, mock_client_class, mock_config, event_manager):
        """Test successful try_login"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        manager.try_login("testuser")
        
        mock_client.send_login_request.assert_called_once()
    
    @patch('src.app.chat_manager.ChatClient')
    def test_try_login_invalid_nickname_short(self, mock_client_class, mock_config, event_manager):
        """Test try_login with invalid nickname (too short)"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        
        event_fired = []
        def handler(data):
            event_fired.append(data)
        
        event_manager.subscribe(EventType.LOGIN_FAILURE, handler)
        manager.try_login("ab")  # Too short (less than 3)
        
        assert len(event_fired) == 1
        mock_client.send_login_request.assert_not_called()
    
    @patch('src.app.chat_manager.ChatClient')
    def test_try_login_invalid_nickname_long(self, mock_client_class, mock_config, event_manager):
        """Test try_login with invalid nickname (too long)"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        
        event_fired = []
        def handler(data):
            event_fired.append(data)
        
        event_manager.subscribe(EventType.LOGIN_FAILURE, handler)
        manager.try_login("a" * 17)  # Too long (more than 16)
        
        assert len(event_fired) == 1
        mock_client.send_login_request.assert_not_called()
    
    @patch('src.app.chat_manager.ChatClient')
    def test_send_message_when_not_logged_in(self, mock_client_class, mock_config, event_manager):
        """Test sending message when not logged in"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        
        event_fired = []
        def handler(data):
            event_fired.append(data)
        
        event_manager.subscribe(EventType.INTERNAL_ERROR, handler)
        manager.send_message("Hello")
        
        assert len(event_fired) == 1
        mock_client.send_chat_text.assert_not_called()
    
    @patch('src.app.chat_manager.ChatClient')
    def test_send_message_when_logged_in(self, mock_client_class, mock_config, event_manager):
        """Test sending message when logged in"""
        mock_client = MagicMock()
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        manager.nickname = "testuser"  # Simulate logged in
        manager.send_message("Hello")
        
        mock_client.send_chat_text.assert_called_once()
    
    def test_send_message_empty(self, mock_config, event_manager):
        """Test sending empty message"""
        manager = ChatManager(mock_config, event_manager)
        manager.nickname = "testuser"
        
        # Should not raise error, just return
        manager.send_message("")
        manager.send_message("   ")
    
    @patch('src.app.chat_manager.ChatClient')
    def test_start_daemon_success(self, mock_client_class, mock_config, event_manager):
        """Test starting daemon thread successfully"""
        import time
        mock_client = MagicMock()
        # Make start_chat run for a short time so thread stays alive
        def start_chat_side_effect():
            time.sleep(0.1)  # Keep thread alive briefly
        mock_client.start_chat = start_chat_side_effect
        mock_client_class.return_value = mock_client
        
        manager = ChatManager(mock_config, event_manager)
        manager.connect()
        manager.start_daemon()
        
        assert manager._chat_thread is not None
        # Give thread a moment to start
        time.sleep(0.01)
        assert manager._chat_thread.is_alive() is True
    
    def test_start_daemon_not_connected(self, mock_config, event_manager):
        """Test starting daemon when not connected"""
        manager = ChatManager(mock_config, event_manager)
        
        event_fired = []
        def handler(data):
            event_fired.append(data)
        
        event_manager.subscribe(EventType.INTERNAL_ERROR, handler)
        manager.start_daemon()
        
        assert manager._chat_thread is None
        assert len(event_fired) == 1
