"""
Tests for EventManager
"""
import pytest
from src.app.events import EventManager, EventType, EventData


@pytest.mark.unit
class TestEventManager:
    """Unit tests for EventManager"""
    
    def test_event_manager_initialization(self):
        """Test EventManager initialization"""
        manager = EventManager()
        assert manager._events is not None
        assert len(manager._events) == 0
    
    def test_subscribe_event(self):
        """Test subscribing to an event"""
        manager = EventManager()
        handler = lambda data: None
        
        manager.subscribe(EventType.USER_JOINED, handler)
        
        assert EventType.USER_JOINED in manager._events
    
    def test_subscribe_multiple_handlers(self):
        """Test subscribing multiple handlers to same event"""
        manager = EventManager()
        handler1 = lambda data: None
        handler2 = lambda data: None
        
        manager.subscribe(EventType.USER_JOINED, handler1)
        manager.subscribe(EventType.USER_JOINED, handler2)
        
        assert EventType.USER_JOINED in manager._events
    
    def test_unsubscribe_event(self):
        """Test unsubscribing from an event"""
        manager = EventManager()
        handler = lambda data: None
        
        manager.subscribe(EventType.USER_JOINED, handler)
        manager.unsubscribe(EventType.USER_JOINED, handler)
        
        # Handler should be removed from _handlers list
        hook = manager._events.get(EventType.USER_JOINED)
        assert hook is not None
        assert handler not in hook._handlers
    
    def test_unsubscribe_nonexistent_event(self):
        """Test unsubscribing from non-existent event (should not raise error)"""
        manager = EventManager()
        handler = lambda data: None
        
        # Should not raise error
        manager.unsubscribe(EventType.USER_JOINED, handler)
    
    def test_fire_event_with_handler(self):
        """Test firing an event with registered handler"""
        manager = EventManager()
        handler_called = []
        handler = lambda data: handler_called.append(data)
        
        manager.subscribe(EventType.USER_JOINED, handler)
        manager.fire(EventType.USER_JOINED, data="test_data")
        
        assert len(handler_called) == 1
        assert isinstance(handler_called[0], EventData)
    
    def test_fire_event_without_handler(self):
        """Test firing an event without registered handler (should not raise error)"""
        manager = EventManager()
        
        # Should not raise error
        manager.fire(EventType.USER_JOINED, data="test_data")
    
    def test_fire_event_with_multiple_handlers(self):
        """Test firing an event with multiple handlers"""
        manager = EventManager()
        handler1_called = []
        handler2_called = []
        
        handler1 = lambda data: handler1_called.append(data)
        handler2 = lambda data: handler2_called.append(data)
        
        manager.subscribe(EventType.USER_JOINED, handler1)
        manager.subscribe(EventType.USER_JOINED, handler2)
        manager.fire(EventType.USER_JOINED, data="test_data")
        
        assert len(handler1_called) == 1
        assert len(handler2_called) == 1
    
    def test_fire_event_with_source_component(self):
        """Test firing an event with source component"""
        manager = EventManager()
        handler_called = []
        handler = lambda data: handler_called.append(data)
        
        manager.subscribe(EventType.USER_JOINED, handler)
        manager.fire(EventType.USER_JOINED, data="test_data", source_component="test_component")
        
        assert len(handler_called) == 1
        assert handler_called[0].source_component == "test_component"
    
    def test_fire_event_with_message(self):
        """Test firing an event with message"""
        manager = EventManager()
        handler_called = []
        handler = lambda data: handler_called.append(data)
        
        manager.subscribe(EventType.USER_JOINED, handler)
        manager.fire(EventType.USER_JOINED, data="test_data", message="test_message")
        
        assert len(handler_called) == 1
        assert handler_called[0].message == "test_message"
    
    def test_multiple_event_types(self):
        """Test managing multiple event types"""
        manager = EventManager()
        handler1 = lambda data: None
        handler2 = lambda data: None
        
        manager.subscribe(EventType.USER_JOINED, handler1)
        manager.subscribe(EventType.USER_LEFT, handler2)
        
        assert EventType.USER_JOINED in manager._events
        assert EventType.USER_LEFT in manager._events
