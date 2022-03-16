import time
import unittest

import docker
from alembic import command
from alembic.config import Config
from sqlalchemy.exc import OperationalError

from model import data
from utils import config


class ComparingWithDictTest(unittest.TestCase):
    def assertObjectsEqualByDictCasting(self, object1, object2, keys):
        object1_dict = {key: object1.__dict__[key] for key in keys}
        object2_dict = {key: object2.__dict__[key] for key in keys}

        self.assertDictEqual(object1_dict, object2_dict)


class DbTest(ComparingWithDictTest):
    @classmethod
    def setUpClass(cls) -> None:

        # Create a postgres docker container for the tests
        client = docker.from_env()
        pgsql_container = client.containers.run(
            name=f'ssl-crawler-db-test-{cls.__name__}',
            image='postgres:11.4',
            environment=[f'POSTGRES_DB={config.get_env("SSL_CRAWLER_DB_NAME")}',
                         f'POSTGRES_USER={config.get_env("SSL_CRAWLER_DB_USER")}',
                         f'POSTGRES_PASSWORD={config.get_env("SSL_CRAWLER_DB_PASSWORD")}'],
            ports={'5432': config.get_env('SSL_CRAWLER_DB_PORT')},
            detach=True
        )

        # Wait for the postgres to be ready (2 sec), maybe there is a better way but the docker exec
        # gives a warning about unclosed file descriptor
        print(f"Starting {pgsql_container}")
        time.sleep(2)
        print(f"Started {pgsql_container}")

        client.close()

    @classmethod
    def tearDownClass(cls) -> None:
        client = docker.from_env()
        pgsql_container = client.containers.get(f'ssl-crawler-db-test-{cls.__name__}')

        print(f"Will stop {pgsql_container}")
        pgsql_container.stop()
        pgsql_container.remove()
        client.close()
        time.sleep(3.5)
        print(f"Stopped {pgsql_container}")

    def tearDown(self) -> None:
        data.close_session()

        with data.engine.connect() as connexion:
            connexion.execute(f"drop schema if exists {config.get_env('SSL_CRAWLER_DB_SCHEMA')} cascade;")

    def setUp(self) -> None:
        self.alembic_upgrade_done = False
        alembic_cfg = Config("alembic.ini")
        alembic_cfg.set_section_option('alembic', 'sqlalchemy.url', config.get_db_string())
        done = False
        while not done:
            try:
                command.upgrade(alembic_cfg, "head")
                done = True
                self.alembic_upgrade_done = True
                print("Alembic upgrade done")
            except OperationalError:
                print("Wait for postgres container to startup")
                time.sleep(0.5)

