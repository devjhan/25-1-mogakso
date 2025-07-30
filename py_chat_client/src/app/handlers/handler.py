from abc import ABC, abstractmethod
from pydantic import BaseModel
from typing import TYPE_CHECKING, Type

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class Handler(ABC):
    @abstractmethod
    def handle(self, manager: ChatManager, dto: BaseModel) -> None:
        pass

    @abstractmethod
    def get_type(self) -> Type[BaseModel]:
        pass
