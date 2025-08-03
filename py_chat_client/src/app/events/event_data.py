from typing import Any, Optional
from pydantic import BaseModel
from src.app.events.event_type import EventType

class EventData(BaseModel):
    event_type: EventType
    data: Any
    source_component: Optional[str] = None
    message: Optional[str] = None