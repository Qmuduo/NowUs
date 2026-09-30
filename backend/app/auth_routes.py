from datetime import timedelta
import hmac
import re
import uuid

from fastapi import APIRouter, Depends, HTTPException, Request, Response
from pydantic import BaseModel, ConfigDict

from .auth import digest_secret, generate_otp, generate_session_token, normalize_email
from .db import connect
from .security import current_user, now


router = APIRouter(prefix="/v1/auth")
_EMAIL = re.compile(r"[^\s@]+@[^\s@]+\.[^\s@]+\Z")


class Input(BaseModel):
    model_config = ConfigDict(extra="forbid")


class OtpRequest(Input):
    email: str


class OtpVerification(Input):
    email: str
    code: str


def _error(status: int, code: str, **details):
    raise HTTPException(status_code=status, detail={"code": code, **details})


def _validate_email(email: str) -> str:
    normalized = normalize_email(email)
    if len(normalized) > 254 or not _EMAIL.fullmatch(normalized):
        _error(422, "email_invalid")
    return normalized


@router.post("/otp", status_code=202)
def request_otp(body: OtpRequest, request: Request):
    email = _validate_email(body.email)
    timestamp = now(request)
    source_ip = request.client.host if request.client else "unknown"
    settings = request.app.state.settings
    with connect(settings.database_url) as database:
        for lock_key in sorted((f"email:{email}", f"ip:{source_ip}")):
            database.execute("SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))", (lock_key,))
        last = database.execute(
            "SELECT created_at FROM otp_challenges WHERE email = %s ORDER BY created_at DESC LIMIT 1",
            (email,),
        ).fetchone()
        if last and timestamp - last["created_at"] < timedelta(seconds=60):
            retry_after = max(1, 60 - int((timestamp - last["created_at"]).total_seconds()))
            _error(429, "otp_resend_wait", retryAfterSeconds=retry_after)
        email_count = database.execute(
            "SELECT count(*) AS count FROM otp_challenges WHERE email = %s AND created_at > %s",
            (email, timestamp - timedelta(hours=1)),
        ).fetchone()["count"]
        ip_count = database.execute(
            "SELECT count(*) AS count FROM otp_challenges WHERE request_ip = %s AND created_at > %s",
            (source_ip, timestamp - timedelta(hours=1)),
        ).fetchone()["count"]
        if email_count >= 5:
            _error(429, "otp_hourly_limit")
        if ip_count >= 30:
            _error(429, "otp_source_hourly_limit")
        code = generate_otp()
        digest = digest_secret(f"otp:{email}:{code}", settings.app_secret)
        database.execute(
            "UPDATE otp_challenges SET consumed_at = %s WHERE email = %s AND consumed_at IS NULL",
            (timestamp, email),
        )
        database.execute(
            "INSERT INTO otp_challenges (email, code_digest, created_at, expires_at, request_ip) "
            "VALUES (%s, %s, %s, %s, %s)",
            (email, digest, timestamp, timestamp + timedelta(minutes=10), source_ip),
        )
    try:
        request.app.state.mailer.send_otp(email, code)
    except Exception as error:
        with connect(settings.database_url) as database:
            database.execute(
                "UPDATE otp_challenges SET consumed_at = %s WHERE email = %s AND code_digest = %s AND consumed_at IS NULL",
                (timestamp, email, digest),
            )
        _error(503, "mail_send_failed")
    return {"retryAfterSeconds": 60}


@router.post("/verify")
def verify_otp(body: OtpVerification, request: Request):
    email = _validate_email(body.email)
    if not re.fullmatch(r"\d{6}", body.code):
        _error(422, "otp_format_invalid")
    timestamp = now(request)
    settings = request.app.state.settings
    session_token = generate_session_token()
    session_id = uuid.uuid4().hex
    failure: tuple[int, str, dict] | None = None
    with connect(settings.database_url) as database:
        challenge = database.execute(
            "SELECT id, code_digest, expires_at, attempts, consumed_at FROM otp_challenges "
            "WHERE email = %s ORDER BY created_at DESC LIMIT 1 FOR UPDATE",
            (email,),
        ).fetchone()
        if challenge is None:
            failure = (400, "otp_missing", {})
        elif challenge["attempts"] >= 5:
            failure = (429, "otp_attempts_exceeded", {})
        elif challenge["consumed_at"] is not None:
            failure = (400, "otp_missing", {})
        elif challenge["expires_at"] <= timestamp:
            database.execute("UPDATE otp_challenges SET consumed_at = %s WHERE id = %s", (timestamp, challenge["id"]))
            failure = (410, "otp_expired", {})
        elif not hmac.compare_digest(digest_secret(f"otp:{email}:{body.code}", settings.app_secret), challenge["code_digest"]):
            attempts = challenge["attempts"] + 1
            consumed = timestamp if attempts >= 5 else None
            database.execute(
                "UPDATE otp_challenges SET attempts = %s, consumed_at = %s WHERE id = %s",
                (attempts, consumed, challenge["id"]),
            )
            if attempts >= 5:
                failure = (429, "otp_attempts_exceeded", {})
            else:
                failure = (400, "otp_invalid", {"remainingAttempts": 5 - attempts})
        if failure is None:
            database.execute("UPDATE otp_challenges SET consumed_at = %s WHERE id = %s", (timestamp, challenge["id"]))
            user = database.execute(
                "SELECT id FROM user_accounts WHERE email = %s FOR UPDATE", (email,)
            ).fetchone()
            user_id = user["id"] if user else uuid.uuid4().hex
            if user is None:
                database.execute("INSERT INTO user_accounts (id, email, created_at) VALUES (%s, %s, %s)", (user_id, email, timestamp))
            token_digest = digest_secret(f"session:{session_token}", settings.app_secret)
            database.execute(
                "INSERT INTO sessions (id, user_id, token_digest, created_at, expires_at) VALUES (%s, %s, %s, %s, %s)",
                (session_id, user_id, token_digest, timestamp, timestamp + timedelta(days=30)),
            )
    if failure is not None:
        _error(failure[0], failure[1], **failure[2])
    return {"accessToken": session_token, "expiresAt": (timestamp + timedelta(days=30)).isoformat(), "userId": user_id}


@router.delete("/session", status_code=204)
def logout(request: Request, user: dict = Depends(current_user)):
    with connect(request.app.state.settings.database_url) as database:
        database.execute("UPDATE sessions SET revoked_at = %s WHERE id = %s", (now(request), request.state.session_id))
    return Response(status_code=204)
