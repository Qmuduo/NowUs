from datetime import datetime, timezone

from fastapi import Depends, HTTPException, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .auth import digest_secret
from .db import connect


bearer = HTTPBearer(auto_error=False)


def now(request: Request) -> datetime:
    clock = getattr(request.app.state, "clock", None)
    return clock() if clock else datetime.now(timezone.utc)


def current_user(
    request: Request,
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
) -> dict:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(status_code=401, detail={"code": "session_required"})
    settings = request.app.state.settings
    digest = digest_secret(f"session:{credentials.credentials}", settings.app_secret)
    with connect(settings.database_url) as database:
        row = database.execute(
            "SELECT s.id AS session_id, u.id AS user_id, u.email "
            "FROM sessions s JOIN user_accounts u ON u.id = s.user_id "
            "WHERE s.token_digest = %s AND s.revoked_at IS NULL AND s.expires_at > %s",
            (digest, now(request)),
        ).fetchone()
    if row is None:
        raise HTTPException(status_code=401, detail={"code": "session_invalid"})
    request.state.session_id = row["session_id"]
    return {"id": row["user_id"], "email": row["email"]}
