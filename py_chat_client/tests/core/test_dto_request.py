"""
Tests for DTO request models
"""
import pytest
from pydantic import ValidationError
from src.core.dto import (
    ChatTextRequest,
    UserLoginRequest,
    FileStartRequest,
    FileEndRequest
)


@pytest.mark.unit
class TestChatTextRequest:
    """Unit tests for ChatTextRequest"""
    
    def test_create_valid_chat_text_request(self):
        """Test creating a valid ChatTextRequest"""
        request = ChatTextRequest(message="Hello, World!")
        assert request.message == "Hello, World!"
    
    def test_create_empty_message(self):
        """Test creating ChatTextRequest with empty message"""
        request = ChatTextRequest(message="")
        assert request.message == ""
    
    def test_create_blank_message(self):
        """Test creating ChatTextRequest with blank message"""
        request = ChatTextRequest(message="   ")
        assert request.message == "   "
    
    def test_chat_text_request_is_frozen(self):
        """Test that ChatTextRequest is immutable"""
        request = ChatTextRequest(message="test")
        with pytest.raises(ValidationError):
            request.message = "modified"
    
    def test_chat_text_request_json_serialization(self):
        """Test JSON serialization of ChatTextRequest"""
        request = ChatTextRequest(message="Hello")
        json_str = request.model_dump_json()
        assert "Hello" in json_str
        assert "message" in json_str


@pytest.mark.unit
class TestUserLoginRequest:
    """Unit tests for UserLoginRequest"""
    
    def test_create_valid_user_login_request(self):
        """Test creating a valid UserLoginRequest"""
        request = UserLoginRequest(nickname="testuser")
        assert request.nickname == "testuser"
    
    def test_create_empty_nickname(self):
        """Test creating UserLoginRequest with empty nickname"""
        request = UserLoginRequest(nickname="")
        assert request.nickname == ""
    
    def test_create_blank_nickname(self):
        """Test creating UserLoginRequest with blank nickname"""
        request = UserLoginRequest(nickname="   ")
        assert request.nickname == "   "
    
    def test_user_login_request_is_frozen(self):
        """Test that UserLoginRequest is immutable"""
        request = UserLoginRequest(nickname="testuser")
        with pytest.raises(ValidationError):
            request.nickname = "modified"
    
    def test_user_login_request_json_serialization(self):
        """Test JSON serialization of UserLoginRequest"""
        request = UserLoginRequest(nickname="testuser")
        json_str = request.model_dump_json()
        assert "testuser" in json_str
        assert "nickname" in json_str


@pytest.mark.unit
class TestFileStartRequest:
    """Unit tests for FileStartRequest"""
    
    def test_create_valid_file_start_request(self):
        """Test creating a valid FileStartRequest"""
        request = FileStartRequest(filename="test.txt", filesize=1024)
        assert request.filename == "test.txt"
        assert request.filesize == 1024
    
    def test_create_file_start_request_zero_size(self):
        """Test creating FileStartRequest with zero size"""
        request = FileStartRequest(filename="test.txt", filesize=0)
        assert request.filesize == 0
    
    def test_create_file_start_request_large_size(self):
        """Test creating FileStartRequest with large size"""
        request = FileStartRequest(filename="test.txt", filesize=1024 * 1024 * 1024)
        assert request.filesize == 1024 * 1024 * 1024
    
    def test_create_file_start_request_empty_filename(self):
        """Test creating FileStartRequest with empty filename"""
        request = FileStartRequest(filename="", filesize=100)
        assert request.filename == ""
    
    def test_file_start_request_is_frozen(self):
        """Test that FileStartRequest is immutable"""
        request = FileStartRequest(filename="test.txt", filesize=100)
        with pytest.raises(ValidationError):
            request.filename = "modified.txt"
    
    def test_file_start_request_json_serialization(self):
        """Test JSON serialization of FileStartRequest"""
        request = FileStartRequest(filename="test.txt", filesize=1024)
        json_str = request.model_dump_json()
        assert "test.txt" in json_str
        assert "1024" in json_str


@pytest.mark.unit
class TestFileEndRequest:
    """Unit tests for FileEndRequest"""
    
    def test_create_valid_file_end_request(self):
        """Test creating a valid FileEndRequest"""
        request = FileEndRequest(filename="test.txt", checksum="abc123")
        assert request.filename == "test.txt"
        assert request.checksum == "abc123"
    
    def test_create_file_end_request_empty_checksum(self):
        """Test creating FileEndRequest with empty checksum"""
        request = FileEndRequest(filename="test.txt", checksum="")
        assert request.checksum == ""
    
    def test_create_file_end_request_long_checksum(self):
        """Test creating FileEndRequest with long checksum (SHA256)"""
        checksum = "a" * 64  # SHA256 hex length
        request = FileEndRequest(filename="test.txt", checksum=checksum)
        assert request.checksum == checksum
    
    def test_file_end_request_is_frozen(self):
        """Test that FileEndRequest is immutable"""
        request = FileEndRequest(filename="test.txt", checksum="abc")
        with pytest.raises(ValidationError):
            request.checksum = "modified"
    
    def test_file_end_request_json_serialization(self):
        """Test JSON serialization of FileEndRequest"""
        request = FileEndRequest(filename="test.txt", checksum="abc123")
        json_str = request.model_dump_json()
        assert "test.txt" in json_str
        assert "abc123" in json_str
