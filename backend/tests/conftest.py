from datetime import datetime, timezone
import threading

import pytest
from fastapi.testclient import TestClient

from backend.app.config import Settings
from backend.app.db import connect
from backend.app.main import create_app


class CaptureMailer:
    def __init__(self):
        self.messages = []
        self.lock = threading.Lock()

    def send_otp(self, email: str, code: str) -> None:
        with self.lock:
            self.messages.append((email, code))

    def latest(self, email: str) -> str:
        with self.lock:
            return next(code for address, code in reversed(self.messages) if address == email)


@pytest.fixture
def api():
    settings = Settings.from_env()
    mailer = CaptureMailer()
    application = create_app(settings=settings, mailer=mailer)
    with TestClient(application) as client:
        with connect(settings.database_url) as database:
            database.execute("TRUNCATE otp_challenges")
            database.execute("TRUNCATE user_accounts CASCADE")
        client.app.state.clock = lambda: datetime(2026, 9, 30, 4, tzinfo=timezone.utc)
        yield client, mailer
