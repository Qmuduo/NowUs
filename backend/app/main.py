from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI

from .auth_routes import router as auth_router
from .config import Settings
from .data_routes import router as data_router
from .db import apply_migrations
from .mail import SmtpMailer
from .pairing_routes import router as pairing_router


def create_app(*, settings: Settings | None = None, mailer=None, strict: bool = False) -> FastAPI:
    config = settings or Settings.from_env()

    @asynccontextmanager
    async def lifespan(application: FastAPI):
        application.state.settings = config
        application.state.mailer = mailer or SmtpMailer(config)
        application.state.clock = lambda: datetime.now(timezone.utc)
        if strict or config.database_url:
            config.validate()
            apply_migrations(config.database_url)
        yield

    application = FastAPI(lifespan=lifespan, docs_url=None if strict else "/docs")

    @application.get("/health")
    def health():
        return {"status": "ok"}

    application.include_router(auth_router)
    application.include_router(pairing_router)
    application.include_router(data_router)
    return application


app = create_app(strict=True)
