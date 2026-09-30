"""OTP and session helpers."""

import hashlib
import hmac
import secrets


def generate_otp() -> str:
    return "".join(str(secrets.randbelow(10)) for _ in range(6))


def generate_session_token() -> str:
    return secrets.token_urlsafe(32)


def digest_secret(value: str, key: str | bytes) -> str:
    secret = key.encode("utf-8") if isinstance(key, str) else key
    return hmac.new(secret, value.encode("utf-8"), hashlib.sha256).hexdigest()


def normalize_email(email: str) -> str:
    return email.strip().lower()
