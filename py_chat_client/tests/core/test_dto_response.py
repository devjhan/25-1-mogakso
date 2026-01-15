"""
Tests for DTO response models
"""
import pytest
from datetime import datetime
from pydantic import ValidationError
from src.core.dto import (
    ChatTextBroadcast,
    SystemNoticeBroadcast,
    ErrorResponse,
    FileEndBroadcast,
    FileStartBroadcast,
    UserJoinBroadcast,
    UserLeaveBroadcast,
    UserLoginResponse
)


@pytest.mark.unit
class TestChatTextBroadcast:
    """Unit tests for ChatTextBroadcast"""
    
    def test_create_valid_chat_text_broadcast(self):
        """Test creating a valid ChatTextBroadcast"""
        now = datetime.now()
        broadcast = ChatTextBroadcast(author="user1", content="Hello", timestamp=now)
        assert broadcast.author == "user1"
        assert broadcast.content == "Hello"
        assert broadcast.timestamp == now
    
    def test_chat_text_broadcast_is_frozen(self):
        """Test that ChatTextBroadcast is immutable"""
        broadcast = ChatTextBroadcast(
            author="user1",
            content="Hello",
            timestamp=datetime.now()
        )
        with pytest.raises(ValidationError):
            broadcast.content = "modified"


@pytest.mark.unit
class TestSystemNoticeBroadcast:
    """Unit tests for SystemNoticeBroadcast"""
    
    def test_create_valid_system_notice_broadcast(self):
        """Test creating a valid SystemNoticeBroadcast"""
        broadcast = SystemNoticeBroadcast(notice="System message")
        assert broadcast.notice == "System message"
    
    def test_system_notice_broadcast_is_frozen(self):
        """Test that SystemNoticeBroadcast is immutable"""
        broadcast = SystemNoticeBroadcast(notice="test")
        with pytest.raises(ValidationError):
            broadcast.notice = "modified"


@pytest.mark.unit
class TestErrorResponse:
    """Unit tests for ErrorResponse"""
    
    def test_create_valid_error_response(self):
        """Test creating a valid ErrorResponse"""
        now = datetime.now()
        error = ErrorResponse(errorCode="AUTH_REQUIRED", message="Login required", timestamp=now)
        assert error.errorCode == "AUTH_REQUIRED"
        assert error.message == "Login required"
        assert error.timestamp == now
    
    def test_error_response_is_frozen(self):
        """Test that ErrorResponse is immutable"""
        error = ErrorResponse(
            errorCode="ERROR",
            message="test",
            timestamp=datetime.now()
        )
        with pytest.raises(ValidationError):
            error.message = "modified"


@pytest.mark.unit
class TestUserLoginResponse:
    """Unit tests for UserLoginResponse"""
    
    def test_create_valid_user_login_response_success(self):
        """Test creating a valid successful UserLoginResponse"""
        response = UserLoginResponse(success=True, message="Login successful", nickname="user1", clientId=1)
        assert response.success is True
        assert response.message == "Login successful"
        assert response.nickname == "user1"
        assert response.clientId == 1
    
    def test_create_valid_user_login_response_failure(self):
        """Test creating a valid failed UserLoginResponse"""
        response = UserLoginResponse(success=False, message="Login failed", nickname="", clientId=-1)
        assert response.success is False
        assert response.message == "Login failed"
        assert response.nickname == ""
        assert response.clientId == -1
    
    def test_user_login_response_is_frozen(self):
        """Test that UserLoginResponse is immutable"""
        response = UserLoginResponse(success=True, message="test", nickname="user", clientId=1)
        with pytest.raises(ValidationError):
            response.success = False


@pytest.mark.unit
class TestFileEndBroadcast:
    """Unit tests for FileEndBroadcast"""
    
    def test_create_valid_file_end_broadcast(self):
        """Test creating a valid FileEndBroadcast"""
        broadcast = FileEndBroadcast(filename="test.txt", status="COMPLETED")
        assert broadcast.filename == "test.txt"
        assert broadcast.status == "COMPLETED"
    
    def test_file_end_broadcast_is_frozen(self):
        """Test that FileEndBroadcast is immutable"""
        broadcast = FileEndBroadcast(filename="test.txt", status="COMPLETED")
        with pytest.raises(ValidationError):
            broadcast.status = "FAILED"


@pytest.mark.unit
class TestFileStartBroadcast:
    """Unit tests for FileStartBroadcast"""
    
    def test_create_valid_file_start_broadcast(self):
        """Test creating a valid FileStartBroadcast"""
        broadcast = FileStartBroadcast(senderNickname="user1", filename="test.txt", status="STARTED")
        assert broadcast.senderNickname == "user1"
        assert broadcast.filename == "test.txt"
        assert broadcast.status == "STARTED"
    
    def test_file_start_broadcast_is_frozen(self):
        """Test that FileStartBroadcast is immutable"""
        broadcast = FileStartBroadcast(senderNickname="user1", filename="test.txt", status="STARTED")
        with pytest.raises(ValidationError):
            broadcast.status = "FAILED"


@pytest.mark.unit
class TestUserJoinBroadcast:
    """Unit tests for UserJoinBroadcast"""
    
    def test_create_valid_user_join_broadcast(self):
        """Test creating a valid UserJoinBroadcast"""
        broadcast = UserJoinBroadcast(nickname="user1")
        assert broadcast.nickname == "user1"
    
    def test_user_join_broadcast_is_frozen(self):
        """Test that UserJoinBroadcast is immutable"""
        broadcast = UserJoinBroadcast(nickname="user1")
        with pytest.raises(ValidationError):
            broadcast.nickname = "user2"


@pytest.mark.unit
class TestUserLeaveBroadcast:
    """Unit tests for UserLeaveBroadcast"""
    
    def test_create_valid_user_leave_broadcast(self):
        """Test creating a valid UserLeaveBroadcast"""
        broadcast = UserLeaveBroadcast(nickname="user1")
        assert broadcast.nickname == "user1"
    
    def test_user_leave_broadcast_is_frozen(self):
        """Test that UserLeaveBroadcast is immutable"""
        broadcast = UserLeaveBroadcast(nickname="user1")
        with pytest.raises(ValidationError):
            broadcast.nickname = "user2"
