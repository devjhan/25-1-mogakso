from collections import defaultdict
from typing import Callable, Optional, Any
from . import EventType
from .event_data import EventData
from .event_hook import _EventHook

class EventManager:
    def __init__(self):
        self._events: dict[EventType, _EventHook] = defaultdict(_EventHook)

    def subscribe(self, event_type: EventType, handler: Callable):
        self._events[event_type] += handler

    def unsubscribe(self, event_type: EventType, handler: Callable):
        if event_type in self._events:
            self._events[event_type] -= handler

    def fire(self, event_type: EventType, data: Any = None, source_component: Optional[str] = None, message: Optional[str] = None):
        if event_type in self._events:
            event_payload = EventData(event_type=event_type, data=data, source_component=source_component, message=message)
            self._events[event_type].fire(event_payload)


event_manager = EventManager()
