"""
Tests for MessageType enum
"""
import pytest
from src.core.enums import MessageType


@pytest.mark.unit
class TestMessageType:
    """Unit tests for MessageType enum"""
    
    def test_all_message_types_have_unique_values(self):
        """Test that all message types have unique integer values"""
        values = [mt.value for mt in MessageType]
        assert len(values) == len(set(values)), "MessageType values must be unique"
    
    def test_get_valid_message_type_values(self):
        """Test getting valid message type values"""
        assert MessageType.MSG_TYPE_CHAT_TEXT.value == 1
        assert MessageType.MSG_TYPE_FILE_INFO.value == 10
        assert MessageType.MSG_TYPE_FILE_CHUNK.value == 11
        assert MessageType.MSG_TYPE_FILE_END.value == 12
        assert MessageType.MSG_TYPE_USER_LOGIN_REQUEST.value == 100
        assert MessageType.MSG_TYPE_USER_LOGIN_RESPONSE.value == 101
        assert MessageType.MSG_TYPE_USER_JOIN_NOTICE.value == 200
        assert MessageType.MSG_TYPE_USER_LEAVE_NOTICE.value == 201
        assert MessageType.MSG_TYPE_SERVER_NOTICE.value == 202
        assert MessageType.MSG_TYPE_ERROR_RESPONSE.value == 500
        assert MessageType.MSG_TYPE_PING.value == 900
        assert MessageType.MSG_TYPE_PONG.value == 901
    
    def test_message_type_from_value(self):
        """Test creating MessageType from integer value"""
        assert MessageType(1) == MessageType.MSG_TYPE_CHAT_TEXT
        assert MessageType(100) == MessageType.MSG_TYPE_USER_LOGIN_REQUEST
        assert MessageType(500) == MessageType.MSG_TYPE_ERROR_RESPONSE
    
    def test_invalid_message_type_value(self):
        """Test that invalid value raises ValueError"""
        with pytest.raises(ValueError):
            MessageType(999)
    
    def test_message_type_ordering(self):
        """Test that MessageType values maintain expected ordering"""
        assert MessageType.MSG_TYPE_CHAT_TEXT.value < MessageType.MSG_TYPE_FILE_INFO.value
        assert MessageType.MSG_TYPE_FILE_INFO.value < MessageType.MSG_TYPE_USER_LOGIN_REQUEST.value
        assert MessageType.MSG_TYPE_USER_LOGIN_REQUEST.value < MessageType.MSG_TYPE_USER_JOIN_NOTICE.value
    
    def test_all_message_types_are_positive(self):
        """Test that all message type values are positive"""
        for msg_type in MessageType:
            assert msg_type.value > 0, f"MessageType {msg_type.name} has non-positive value: {msg_type.value}"
