from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    environment: str = "development"
    database_url: str = ""
    app_secret: str = ""
    smtp_host: str = ""
    smtp_port: int = 1025
    smtp_from: str = ""
    smtp_username: str = ""
    smtp_password: str = ""
    smtp_starttls: bool = False
    dev_mailbox: bool = False

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            environment=os.getenv("NOWUS_ENV", "development").strip().lower(),
            database_url=os.getenv("NOWUS_DATABASE_URL", ""),
            app_secret=os.getenv("NOWUS_APP_SECRET", ""),
            smtp_host=os.getenv("NOWUS_SMTP_HOST", ""),
            smtp_port=int(os.getenv("NOWUS_SMTP_PORT_INTERNAL", os.getenv("NOWUS_SMTP_PORT", "1025"))),
            smtp_from=os.getenv("NOWUS_SMTP_FROM", ""),
            smtp_username=os.getenv("NOWUS_SMTP_USERNAME", ""),
            smtp_password=os.getenv("NOWUS_SMTP_PASSWORD", ""),
            smtp_starttls=os.getenv("NOWUS_SMTP_STARTTLS", "false").lower() == "true",
            dev_mailbox=os.getenv("NOWUS_DEV_MAILBOX", "false").lower() == "true",
        )

    def validate(self) -> None:
        missing = [name for name, value in (
            ("NOWUS_DATABASE_URL", self.database_url),
            ("NOWUS_APP_SECRET", self.app_secret),
            ("NOWUS_SMTP_HOST", self.smtp_host),
            ("NOWUS_SMTP_FROM", self.smtp_from),
        ) if not value]
        if missing:
            raise RuntimeError("missing required settings: " + ", ".join(missing))
        if len(self.app_secret) < 32:
            raise RuntimeError("NOWUS_APP_SECRET must contain at least 32 characters")
        if self.dev_mailbox and self.environment != "development":
            raise RuntimeError("NOWUS_DEV_MAILBOX is only available in development")
        if self.smtp_starttls and not self.smtp_host:
            raise RuntimeError("SMTP STARTTLS requires an SMTP host")
        if (self.smtp_username and not self.smtp_password) or (self.smtp_password and not self.smtp_username):
            raise RuntimeError("SMTP username and password must be configured together")
        if self.environment == "production" and not self.smtp_starttls:
            raise RuntimeError("production SMTP must use STARTTLS")
