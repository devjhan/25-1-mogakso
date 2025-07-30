from pydantic import BaseModel, ConfigDict

class ChatTextRequest(BaseModel):
    message: str
    model_config = ConfigDict(frozen=True)

class FileStartRequest(BaseModel):
    filename: str
    filesize: int
    model_config = ConfigDict(frozen=True)

class FileEndRequest(BaseModel):
    filename: str
    checksum: str
    model_config = ConfigDict(frozen=True)

class UserLoginRequest(BaseModel):
    nickname: str
    model_config = ConfigDict(frozen=True)