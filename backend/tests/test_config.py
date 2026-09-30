import pytest

from backend.app.config import Settings


def production_settings(**overrides):
    values = {
        "environment": "production",
        "database_url": "postgresql://example/nowus",
        "app_secret": "x" * 40,
        "smtp_host": "smtp.example.com",
        "smtp_port": 587,
        "smtp_from": "login@example.com",
        "smtp_starttls": True,
    }
    values.update(overrides)
    return Settings(**values)


def test_production_requires_starttls():
    with pytest.raises(RuntimeError, match="STARTTLS"):
        production_settings(smtp_starttls=False).validate()


def test_smtp_credentials_must_be_configured_as_a_pair():
    with pytest.raises(RuntimeError, match="configured together"):
        production_settings(smtp_username="sender").validate()


def test_local_mail_sink_is_development_only():
    with pytest.raises(RuntimeError, match="only available in development"):
        production_settings(dev_mailbox=True).validate()


def test_production_smtp_config_is_valid_with_starttls_and_credentials():
    production_settings(smtp_username="sender", smtp_password="secret").validate()
