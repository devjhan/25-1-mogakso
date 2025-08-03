from typing import Type, TYPE_CHECKING
from pydantic import BaseModel
from src.app.events import EventType
from src.app.handlers import Handler
from src.core.dto import ErrorResponse

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class ErrorResponseHandler(Handler):
    def handle(self, manager: "ChatManager", dto: ErrorResponse) -> None:
        manager.event_manager.fire(EventType.INTERNAL_ERROR, data=dto, source_component=self.__class__.__name__, message="오류가 발생했습니다.")

    def get_type(self) -> Type[BaseModel]:
        return ErrorResponse


error_response_handler = ErrorResponseHandler()
