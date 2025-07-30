import datetime
from pydantic import BaseModel, ConfigDict

class ChatTextBroadcast(BaseModel):
    author : str
    content : str
    timestamp : datetime.datetime
    model_config = ConfigDict(frozen=True)

class SystemNoticeBroadcast(BaseModel):
    notice : str
    model_config = ConfigDict(frozen=True)

class ErrorResponse(BaseModel):
    errorCode : str
    message : str
    timestamp : datetime.datetime
    model_config = ConfigDict(frozen=True)

class FileEndBroadcast(BaseModel):
    filename: str
    status: str
    model_config = ConfigDict(frozen=True)

class FileStartBroadcast(BaseModel):
    senderNickname: str
    filename: str
    status: str
    model_config = ConfigDict(frozen=True)

class UserJoinBroadcast(BaseModel):
    nickname: str
    model_config = ConfigDict(frozen=True)

class UserLeaveBroadcast(BaseModel):
    nickname: str
    model_config = ConfigDict(frozen=True)

class UserLoginResponse(BaseModel):
    success: bool
    message: str
    nickname: str
    clientId: int
    model_config = ConfigDict(frozen=True)

