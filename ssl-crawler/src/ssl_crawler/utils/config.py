import os

from dotenv import load_dotenv
from distutils.util import strtobool

if os.getenv("SSL_CRAWLER_ENV") == "test":
    load_dotenv("test.env")
else:
    load_dotenv(".env")


def get_env(name: str, default=None) -> str:
    return os.getenv(name, default)


def get_db_string() -> str:
    db_user = get_env('SSL_CRAWLER_DB_USER')
    if db_user == None:
        # This can happen when working directory (at start up) does not contain a .env file
        print(f"Environment variable SSL_CRAWLER_DB_USER not set => check if you are using the right working directory")
        cwd = os.getcwd()
        raise Exception(f"Environment variable SSL_CRAWLER_DB_USER not set. Current directory: {cwd}")

    return "postgresql://" + get_env('SSL_CRAWLER_DB_USER') + ":" + get_env("SSL_CRAWLER_DB_PASSWORD") + "@" + \
           get_env("SSL_CRAWLER_DB_HOST") + ":" + get_env("SSL_CRAWLER_DB_PORT") + "/" + get_env("SSL_CRAWLER_DB_NAME")


def get_env_bool(name: str, default=False) -> bool:
    try:
        return strtobool(get_env(name))
    except:
        return default
