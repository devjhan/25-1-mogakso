"""
Tests for message handlers
"""
import pytest
from datetime import datetime
from unittest.mock import MagicMock
from src.app.handlers.user_login_response_handler import UserLoginResponseHandler
from src.app.handlers.chat_text_broadcast_handler import chat_text_broadcast_handler
from src.app.handlers.error_response_handler import error_response_handler
from src.app.handlers.system_notice_broadcast_handler import system_notice_broadcast_handler
from src.app.handlers.user_join_broadcast_handler import user_join_broadcast_handler
from src.app.handlers.user_left_broadcast_handler import user_left_broadcast_handler
from src.core.dto import (
    UserLoginResponse,
    ChatTextBroadcast,
    ErrorResponse,
    SystemNoticeBroadcast,
    UserJoinBroadcast,
    UserLeaveBroadcast
)
from src.app.events import EventType


@pytest.mark.unit
class TestUserLoginResponseHandler:
    """Unit tests for UserLoginResponseHandler"""
    
    def test_handle_successful_login(self):
        """Test handling successful login response"""
        handler = UserLoginResponseHandler()
        manager = MagicMock()
        manager.event_manager = MagicMock()
        
        response = UserLoginResponse(success=True, message="Login successful", nickname="testuser", clientId=1)
        handler.handle(manager, response)
        
        assert manager.nickname == "testuser"
        assert manager.client_id == 1
        manager.event_manager.fire.assert_called_once()
        call_args = manager.event_manager.fire.call_args
        assert call_args[0][0] == EventType.LOGIN_SUCCESS
    
    def test_handle_failed_login(self):
        """Test handling failed login response"""
        handler = UserLoginResponseHandler()
        manager = MagicMock()
        manager.event_manager = MagicMock()
        
        response = UserLoginResponse(success=False, message="Login failed", nickname="", clientId=-1)
        handler.handle(manager, response)
        
        assert manager.nickname is None
        manager.event_manager.fire.assert_called_once()
        call_args = manager.event_manager.fire.call_args
        assert call_args[0][0] == EventType.LOGIN_FAILURE
    
    def test_get_type(self):
        """Test get_type returns correct type"""
        handler = UserLoginResponseHandler()
        assert handler.get_type() == UserLoginResponse


@pytest.mark.unit
class TestChatTextBroadcastHandler:
    """Unit tests for ChatTextBroadcastHandler"""
    
    def test_handle_chat_text_broadcast(self):
        """Test handling chat text broadcast"""
        manager = MagicMock()
        manager.nickname = "user2"  # Different from broadcast author
        manager.event_manager = MagicMock()
        
        broadcast = ChatTextBroadcast(
            author="user1",
            content="Hello, World!",
            timestamp=datetime.now()
        )
        
        chat_text_broadcast_handler.handle(manager, broadcast)
        
        manager.event_manager.fire.assert_called_once()
        call_args = manager.event_manager.fire.call_args
        assert call_args[0][0] == EventType.NEW_CHAT_MESSAGE
    
    def test_handle_chat_text_broadcast_same_author(self):
        """Test handling chat text broadcast from same user (should be ignored)"""
        manager = MagicMock()
        manager.nickname = "user1"  # Same as broadcast author
        manager.event_manager = MagicMock()
        
        broadcast = ChatTextBroadcast(
            author="user1",
            content="Hello, World!",
            timestamp=datetime.now()
        )
        
        chat_text_broadcast_handler.handle(manager, broadcast)
        
        # Should not fire event (same author)
        manager.event_manager.fire.assert_not_called()


@pytest.mark.unit
class TestErrorResponseHandler:
    """Unit tests for ErrorResponseHandler"""
    
    def test_handle_error_response(self):
        """Test handling error response"""
        manager = MagicMock()
        manager.event_manager = MagicMock()
        
        error = ErrorResponse(
            errorCode="AUTH_REQUIRED",
            message="Login required",
            timestamp=datetime.now()
        )
        
        error_response_handler.handle(manager, error)
        
        manager.event_manager.fire.assert_called_once()
        call_args = manager.event_manager.fire.call_args
        assert call_args[0][0] == EventType.INTERNAL_ERROR


@pytest.mark.unit
class TestSystemNoticeBroadcastHandler:
    """Unit tests for SystemNoticeBroadcastHandler"""
    
    def test_handle_system_notice(self):
        """Test handling system notice broadcast"""
        manager = MagicMock()
        manager.event_manager = MagicMock()
        
        notice = SystemNoticeBroadcast(notice="System message")
        
        system_notice_broadcast_handler.handle(manager, notice)
        
        manager.event_manager.fire.assert_called_once()
        call_args = manager.event_manager.fire.call_args
        assert call_args[0][0] == EventType.SYSTEM_NOTICE


@pytest.mark.unit
class TestUserJoinBroadcastHandler:
    """Unit tests for UserJoinBroadcastHandler"""
    
    def test_handle_user_join(self):
        """Test handling user join broadcast"""
        manager = MagicMock()
        manager.nickname = "user1"  # Different from join user
        manager.event_manager = MagicMock()
        
        broadcast = UserJoinBroadcast(nickname="newuser")
        
        user_join_broadcast_handler.handle(manager, broadcast)
        
        manager.event_manager.fire.assert_called_once()
        call_args = manager.event_manager.fire.call_args
        assert call_args[0][0] == EventType.USER_JOINED
    
    def test_handle_user_join_same_user(self):
        """Test handling user join broadcast for same user (should be ignored)"""
        manager = MagicMock()
        manager.nickname = "user1"
        manager.event_manager = MagicMock()
        
        broadcast = UserJoinBroadcast(nickname="user1")
        
        user_join_broadcast_handler.handle(manager, broadcast)
        
        # Should not fire event (same user)
        manager.event_manager.fire.assert_not_called()


@pytest.mark.unit
class TestUserLeftBroadcastHandler:
    """Unit tests for UserLeftBroadcastHandler"""
    
    def test_handle_user_left(self):
        """Test handling user left broadcast"""
        manager = MagicMock()
        manager.nickname = "user2"  # Different from left user
        manager.event_manager = MagicMock()
        
        broadcast = UserLeaveBroadcast(nickname="user1")
        
        user_left_broadcast_handler.handle(manager, broadcast)
        
        manager.event_manager.fire.assert_called_once()
        call_args = manager.event_manager.fire.call_args
        assert call_args[0][0] == EventType.USER_LEFT
    
    def test_handle_user_left_same_user(self):
        """Test handling user left broadcast for same user (should be ignored)"""
        manager = MagicMock()
        manager.nickname = "user1"
        manager.event_manager = MagicMock()
        
        broadcast = UserLeaveBroadcast(nickname="user1")
        
        user_left_broadcast_handler.handle(manager, broadcast)
        
        # Should not fire event (same user)
        manager.event_manager.fire.assert_not_called()
