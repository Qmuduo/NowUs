import secrets
import uuid
from datetime import timedelta

from fastapi import APIRouter, Depends, HTTPException, Request, Response
from pydantic import ConfigDict

from .auth import digest_secret
from .auth_routes import Input
from .db import connect
from .security import current_user, now


router = APIRouter(prefix="/v1")
_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
_SCOPE = ["昵称与城市", "通常作息与联系偏好", "临时联系意愿", "当前一条留言"]
_CITY_NAMES = {
    "beijing": "北京", "shanghai": "上海", "new-york": "纽约", "london": "伦敦",
    "paris": "巴黎", "tokyo": "东京", "sydney": "悉尼", "kathmandu": "加德满都",
}


class InviteInput(Input):
    code: str


def _error(status: int, code: str, **details):
    raise HTTPException(status_code=status, detail={"code": code, **details})


def _normalized_code(value: str) -> str:
    return "".join(value.split()).replace("-", "").upper()


def _code_digest(request: Request, code: str) -> str:
    secret = request.app.state.settings.app_secret
    return digest_secret(f"invite:{_normalized_code(code)}", secret)


def _active_invitation(database, request: Request, digest: str):
    row = database.execute(
        "SELECT i.id, i.inviter_id, i.expires_at, i.revoked_at, i.accepted_at, i.accepted_by, "
        "p.name, p.city_id, p.sharing_enabled "
        "FROM invitations i LEFT JOIN profiles p ON p.user_id = i.inviter_id "
        "WHERE i.code_digest = %s",
        (digest,),
    ).fetchone()
    if row is None:
        _error(404, "invite_invalid")
    if row["revoked_at"] is not None:
        _error(410, "invite_revoked")
    if row["accepted_at"] is not None:
        _error(410, "invite_used")
    if row["expires_at"] <= now(request):
        _error(410, "invite_expired")
    if row["sharing_enabled"] is False:
        _error(409, "invite_sharing_paused")
    if not row["name"]:
        _error(409, "invite_profile_missing")
    return row


@router.post("/invites")
def create_invitation(request: Request, user: dict = Depends(current_user)):
    timestamp = now(request)
    with connect(request.app.state.settings.database_url) as database:
        if database.execute("SELECT 1 FROM pair_members WHERE user_id = %s", (user["id"],)).fetchone():
            _error(409, "already_paired")
        profile = database.execute("SELECT name, city_id FROM profiles WHERE user_id = %s", (user["id"],)).fetchone()
        if profile is None:
            _error(409, "profile_required")
        database.execute(
            "UPDATE invitations SET revoked_at = %s WHERE inviter_id = %s AND revoked_at IS NULL AND accepted_at IS NULL",
            (timestamp, user["id"]),
        )
        code = "".join(secrets.choice(_ALPHABET) for _ in range(10))
        database.execute(
            "INSERT INTO invitations (id, inviter_id, code_digest, created_at, expires_at) VALUES (%s, %s, %s, %s, %s)",
            (uuid.uuid4().hex, user["id"], _code_digest(request, code), timestamp, timestamp + timedelta(hours=24)),
        )
    return {
        "code": code,
        "expiresAt": (timestamp + timedelta(hours=24)).isoformat(),
        "inviter": {"name": profile["name"], "cityId": profile["city_id"], "cityName": _CITY_NAMES[profile["city_id"]]},
        "scope": _SCOPE,
    }


@router.get("/invites/{code}")
def preview_invitation(code: str, request: Request):
    with connect(request.app.state.settings.database_url) as database:
        row = _active_invitation(database, request, _code_digest(request, code))
    return {
        "inviter": {"name": row["name"], "cityId": row["city_id"], "cityName": _CITY_NAMES[row["city_id"]]},
        "scope": _SCOPE,
        "expiresAt": row["expires_at"].isoformat(),
    }


@router.delete("/me/invitation", status_code=204)
def revoke_invitation(request: Request, user: dict = Depends(current_user)):
    with connect(request.app.state.settings.database_url) as database:
        database.execute(
            "UPDATE invitations SET revoked_at = %s WHERE inviter_id = %s AND revoked_at IS NULL AND accepted_at IS NULL",
            (now(request), user["id"]),
        )
    return Response(status_code=204)


@router.post("/invites/accept")
def accept_invitation(body: InviteInput, request: Request, user: dict = Depends(current_user)):
    code = _normalized_code(body.code)
    if len(code) != 10 or any(letter not in _ALPHABET for letter in code):
        _error(404, "invite_invalid")
    timestamp = now(request)
    digest = _code_digest(request, code)
    try:
        with connect(request.app.state.settings.database_url) as database:
            row = database.execute(
                "SELECT id, inviter_id, expires_at, revoked_at, accepted_at FROM invitations "
                "WHERE code_digest = %s FOR UPDATE",
                (digest,),
            ).fetchone()
            if row is None:
                _error(404, "invite_invalid")
            if row["inviter_id"] == user["id"]:
                _error(409, "invite_self_accept")
            if row["revoked_at"] is not None:
                _error(410, "invite_revoked")
            if row["accepted_at"] is not None:
                _error(410, "invite_used")
            if row["expires_at"] <= timestamp:
                _error(410, "invite_expired")
            member = database.execute(
                "SELECT user_id FROM pair_members WHERE user_id IN (%s, %s)", (row["inviter_id"], user["id"])
            ).fetchone()
            if member:
                _error(409, "already_paired")
            inviter = database.execute(
                "SELECT name, city_id, sharing_enabled FROM profiles WHERE user_id = %s", (row["inviter_id"],)
            ).fetchone()
            if inviter is None:
                _error(409, "invite_profile_missing")
            if not inviter["sharing_enabled"]:
                _error(409, "invite_sharing_paused")
            pair_id = uuid.uuid4().hex
            database.execute("INSERT INTO pairs (id, created_at) VALUES (%s, %s)", (pair_id, timestamp))
            database.execute(
                "INSERT INTO pair_members (pair_id, user_id) VALUES (%s, %s), (%s, %s)",
                (pair_id, row["inviter_id"], pair_id, user["id"]),
            )
            database.execute(
                "UPDATE invitations SET accepted_by = %s, accepted_at = %s WHERE id = %s",
                (user["id"], timestamp, row["id"]),
            )
            database.execute(
                "UPDATE invitations SET revoked_at = %s WHERE inviter_id IN (%s, %s) "
                "AND id <> %s AND revoked_at IS NULL AND accepted_at IS NULL",
                (timestamp, row["inviter_id"], user["id"], row["id"]),
            )
    except Exception as error:
        try:
            from psycopg import IntegrityError
        except ImportError:
            raise
        if isinstance(error, IntegrityError):
            _error(409, "already_paired")
        raise
    return {"paired": True, "pairId": pair_id}


@router.delete("/pairing", status_code=204)
def unpair(request: Request, user: dict = Depends(current_user)):
    timestamp = now(request)
    with connect(request.app.state.settings.database_url) as database:
        row = database.execute("SELECT pair_id FROM pair_members WHERE user_id = %s", (user["id"],)).fetchone()
        if row is None:
            _error(409, "not_paired")
        participants = [item["user_id"] for item in database.execute(
            "SELECT user_id FROM pair_members WHERE pair_id = %s", (row["pair_id"],)
        ).fetchall()]
        database.execute(
            "UPDATE invitations SET revoked_at = %s WHERE inviter_id = ANY(%s) "
            "AND revoked_at IS NULL AND accepted_at IS NULL",
            (timestamp, participants),
        )
        database.execute("DELETE FROM pairs WHERE id = %s", (row["pair_id"],))
    return Response(status_code=204)
