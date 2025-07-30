from pydantic_settings import BaseSettings, SettingsConfigDict

class Configuration(BaseSettings):
    model_config = SettingsConfigDict(env_file='.env', env_file_encoding='utf-8')

    SERVER_IP : str
    SERVER_PORT : int
    CHUNK_SIZE : int
    DEBUG_MODE : bool

config = Configuration()