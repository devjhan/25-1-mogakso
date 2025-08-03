from typing import Type, TYPE_CHECKING
from src.app.events import EventType
from src.app.handlers.handler import Handler
from src.core.dto import UserLoginResponse

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class UserLoginResponseHandler(Handler):
    def handle(self, manager: "ChatManager", dto: UserLoginResponse) -> None:
        if dto.success:
            manager.nickname = dto.nickname
            manager.client_id = dto.clientId
            manager.event_manager.fire(EventType.LOGIN_SUCCESS, data=dto, source_component=self.__class__.__name__, message="로그인에 성공했습니다.")
        else:
            manager.nickname = None
            manager.event_manager.fire(EventType.LOGIN_FAILURE, data=dto, source_component=self.__class__.__name__, message="로그인에 실패했습니다.")

    def get_type(self) -> Type[UserLoginResponse]:
        return UserLoginResponse


user_login_response_handler = UserLoginResponseHandler()
