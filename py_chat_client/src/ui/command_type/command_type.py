from enum import Enum, unique

@unique
class CommandType(Enum):
    QUIT = "/quit"
    HELP = "/help"
    EXIT = "/exit"
    CLEAR = "/clear"

    USERS = "/users"
    NICKNAME = "/nickname"

    FILE = "/file"
    DOWNLOAD = "/download"
    WHISPER = "/w"
    MESSAGE = "/msg"

    LOGIN = "/login"
    LOGOUT = "/logout"
    CONNECT = "/connect"

    @classmethod
    def from_string(cls, value: str) -> "CommandType" or None:
        for member in cls:
            if member.value == value:
                return member
        return None


