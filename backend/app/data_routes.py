from datetime import timedelta
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Request, Response
from pydantic import BaseModel, ConfigDict, Field

from .db import connect
from .rules import profile_errors, rhythm_errors
from .security import current_user, now


router = APIRouter(prefix="/v1")


class Input(BaseModel):
    model_config = ConfigDict(extra="forbid")


class ProfileInput(Input):
    name: str
    cityId: str


class ScheduleInput(Input):
    weekday: dict[str, Any]
    rest: dict[str, Any]
    templateId: str = "学生"


class NoteInput(Input):
    text: str


class TemporaryInput(Input):
    available: bool
    minutes: int = Field(ge=1, le=180)


class SharingInput(Input):
    enabled: bool


class SetupInput(Input):
    complete: bool


def _error(status: int, code: str, **details):
    raise HTTPException(status_code=status, detail={"code": code, **details})


def _schedule_errors(schedule: dict) -> list[str]:
    errors = rhythm_errors(schedule.get("weekday", {})) + rhythm_errors(schedule.get("rest", {}))
    if schedule.get("templateId", "学生") not in {"学生", "上班族"}:
        errors.append("作息模板类型无效")
    return list(dict.fromkeys(errors))


def _user_values(database, user_id: str, timestamp) -> dict:
    profile = database.execute(
        "SELECT name, city_id, sharing_enabled, setup_complete FROM profiles WHERE user_id = %s", (user_id,)
    ).fetchone()
    schedule = database.execute("SELECT value FROM rhythms WHERE user_id = %s", (user_id,)).fetchone()
    note = database.execute("SELECT text, updated_at FROM notes WHERE user_id = %s", (user_id,)).fetchone()
    temporary = database.execute(
        "SELECT available, updated_at, until_at FROM temporary_statuses WHERE user_id = %s AND until_at > %s",
        (user_id, timestamp),
    ).fetchone()
    return {
        "profile": {"name": profile["name"], "cityId": profile["city_id"]} if profile else None,
        "schedule": schedule["value"] if schedule else None,
        "note": {"text": note["text"], "updatedAt": note["updated_at"].isoformat()} if note else None,
        "temporary": {
            "available": temporary["available"],
            "fromMillis": int(temporary["updated_at"].timestamp() * 1000),
            "untilMillis": int(temporary["until_at"].timestamp() * 1000),
        } if temporary else None,
        "sharingEnabled": profile["sharing_enabled"] if profile else True,
        "setupComplete": profile["setup_complete"] if profile else False,
    }


@router.get("/snapshot")
def snapshot(request: Request, user: dict = Depends(current_user)):
    timestamp = now(request)
    with connect(request.app.state.settings.database_url) as database:
        mine = _user_values(database, user["id"], timestamp)
        membership = database.execute("SELECT pair_id FROM pair_members WHERE user_id = %s", (user["id"],)).fetchone()
        invitation = database.execute(
            "SELECT expires_at FROM invitations WHERE inviter_id = %s AND revoked_at IS NULL "
            "AND accepted_at IS NULL AND expires_at > %s ORDER BY created_at DESC LIMIT 1",
            (user["id"], timestamp),
        ).fetchone()
        partner = None
        pair_status = "not_paired"
        if membership:
            peer = database.execute(
                "SELECT user_id FROM pair_members WHERE pair_id = %s AND user_id <> %s",
                (membership["pair_id"], user["id"]),
            ).fetchone()
            peer_data = _user_values(database, peer["user_id"], timestamp) if peer else None
            if mine["sharingEnabled"] and peer_data and peer_data["sharingEnabled"]:
                pair_status = "shared"
                partner = {key: value for key, value in peer_data.items() if key != "sharingEnabled"}
            else:
                pair_status = "sharing_paused"
        return {
            "userId": user["id"],
            "me": {key: value for key, value in mine.items() if key != "sharingEnabled"},
            "paired": membership is not None,
            "pairStatus": pair_status,
            "sharingEnabled": mine["sharingEnabled"],
            "partner": partner,
            "invitation": {"expiresAt": invitation["expires_at"].isoformat()} if invitation else None,
            "serverTime": timestamp.isoformat(),
        }


@router.put("/me/profile")
def save_profile(body: ProfileInput, request: Request, user: dict = Depends(current_user)):
    normalized = {"name": body.name.strip(), "cityId": body.cityId}
    errors = profile_errors(normalized)
    if errors:
        _error(422, "profile_invalid", messages=errors)
    with connect(request.app.state.settings.database_url) as database:
        database.execute(
            "INSERT INTO profiles (user_id, name, city_id, updated_at) VALUES (%s, %s, %s, %s) "
            "ON CONFLICT (user_id) DO UPDATE SET name = excluded.name, city_id = excluded.city_id, updated_at = excluded.updated_at",
            (user["id"], normalized["name"], normalized["cityId"], now(request)),
        )
    return {"profile": normalized}


@router.put("/me/rhythm")
def save_rhythm(body: ScheduleInput, request: Request, user: dict = Depends(current_user)):
    from psycopg.types.json import Jsonb

    value = {"weekday": body.weekday, "rest": body.rest, "templateId": body.templateId}
    errors = _schedule_errors(value)
    if errors:
        _error(422, "rhythm_invalid", messages=errors)
    with connect(request.app.state.settings.database_url) as database:
        database.execute(
            "INSERT INTO rhythms (user_id, value, updated_at) VALUES (%s, %s, %s) "
            "ON CONFLICT (user_id) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at",
            (user["id"], Jsonb(value), now(request)),
        )
    return {"schedule": value}


@router.put("/me/setup")
def mark_setup(body: SetupInput, request: Request, user: dict = Depends(current_user)):
    with connect(request.app.state.settings.database_url) as database:
        if body.complete:
            profile = database.execute("SELECT 1 FROM profiles WHERE user_id = %s", (user["id"],)).fetchone()
            schedule = database.execute("SELECT 1 FROM rhythms WHERE user_id = %s", (user["id"],)).fetchone()
            if profile is None or schedule is None:
                _error(409, "setup_incomplete")
        updated = database.execute(
            "UPDATE profiles SET setup_complete = %s, updated_at = %s WHERE user_id = %s RETURNING setup_complete",
            (body.complete, now(request), user["id"]),
        ).fetchone()
        if updated is None:
            _error(409, "profile_required")
    return {"setupComplete": updated["setup_complete"]}


@router.put("/me/note")
def save_note(body: NoteInput, request: Request, user: dict = Depends(current_user)):
    if not body.text.strip():
        _error(422, "note_empty")
    if len(body.text) > 120:
        _error(422, "note_too_long", maximum=120)
    timestamp = now(request)
    with connect(request.app.state.settings.database_url) as database:
        database.execute(
            "INSERT INTO notes (user_id, text, updated_at) VALUES (%s, %s, %s) "
            "ON CONFLICT (user_id) DO UPDATE SET text = excluded.text, updated_at = excluded.updated_at",
            (user["id"], body.text, timestamp),
        )
    return {"note": {"text": body.text, "updatedAt": timestamp.isoformat()}}


@router.delete("/me/note", status_code=204)
def delete_note(request: Request, user: dict = Depends(current_user)):
    with connect(request.app.state.settings.database_url) as database:
        database.execute("DELETE FROM notes WHERE user_id = %s", (user["id"],))
    return Response(status_code=204)


@router.put("/me/temporary")
def set_temporary(body: TemporaryInput, request: Request, user: dict = Depends(current_user)):
    if body.minutes not in (30, 60, 180):
        _error(422, "temporary_duration_invalid")
    timestamp = now(request)
    until = timestamp + timedelta(minutes=body.minutes)
    with connect(request.app.state.settings.database_url) as database:
        database.execute(
            "INSERT INTO temporary_statuses (user_id, available, until_at, updated_at) VALUES (%s, %s, %s, %s) "
            "ON CONFLICT (user_id) DO UPDATE SET available = excluded.available, until_at = excluded.until_at, updated_at = excluded.updated_at",
            (user["id"], body.available, until, timestamp),
        )
    return {"available": body.available, "fromMillis": int(timestamp.timestamp() * 1000), "untilMillis": int(until.timestamp() * 1000)}


@router.delete("/me/temporary", status_code=204)
def reset_temporary(request: Request, user: dict = Depends(current_user)):
    with connect(request.app.state.settings.database_url) as database:
        database.execute("DELETE FROM temporary_statuses WHERE user_id = %s", (user["id"],))
    return Response(status_code=204)


@router.put("/me/sharing")
def set_sharing(body: SharingInput, request: Request, user: dict = Depends(current_user)):
    timestamp = now(request)
    with connect(request.app.state.settings.database_url) as database:
        membership = database.execute("SELECT 1 FROM pair_members WHERE user_id = %s", (user["id"],)).fetchone()
        if membership is None:
            _error(409, "not_paired")
        profile = database.execute(
            "UPDATE profiles SET sharing_enabled = %s, updated_at = %s WHERE user_id = %s RETURNING user_id",
            (body.enabled, timestamp, user["id"]),
        ).fetchone()
        if profile is None:
            _error(409, "profile_required")
        if not body.enabled:
            database.execute(
                "UPDATE invitations SET revoked_at = %s WHERE inviter_id = %s "
                "AND revoked_at IS NULL AND accepted_at IS NULL",
                (timestamp, user["id"]),
            )
    return {"sharingEnabled": body.enabled}
