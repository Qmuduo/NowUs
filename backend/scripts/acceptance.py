"""Local end-to-end acceptance through HTTP and the real development SMTP sink."""

import json
import os
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


API = os.getenv("NOWUS_API_URL", "http://127.0.0.1:8000").rstrip("/")
MAILPIT = os.getenv("NOWUS_MAILPIT_URL", "http://127.0.0.1:8025").rstrip("/")


def request(url: str, method: str = "GET", body: dict | None = None, token: str | None = None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"} if data is not None else {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            payload = response.read()
            return response.status, json.loads(payload) if payload else None
    except urllib.error.HTTPError as error:
        payload = error.read()
        try:
            detail = json.loads(payload)
        except ValueError:
            detail = payload.decode(errors="replace")
        return error.code, detail


def require(status: int, expected: int, body):
    assert status == expected, f"expected HTTP {expected}, got {status}: {body}"
    return body


def mailbox_code(email: str) -> str:
    query = urllib.parse.urlencode({"query": f"to:{email}", "limit": "1"})
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        with urllib.request.urlopen(f"{MAILPIT}/api/v1/search?{query}", timeout=5) as response:
            messages = json.loads(response.read()).get("messages", [])
        if messages:
            message_id = urllib.parse.quote(messages[0]["ID"], safe="")
            with urllib.request.urlopen(f"{MAILPIT}/api/v1/message/{message_id}", timeout=5) as response:
                message = json.loads(response.read())
            text = message.get("Text", "")
            match = re.search(r"(?<!\d)(\d{6})(?!\d)", text)
            if match:
                return match.group(1)
        time.sleep(0.25)
    raise AssertionError(f"OTP email was not visible in the local Mailpit inbox for {email}")


def login(email: str) -> tuple[str, str]:
    status, response = request(f"{API}/v1/auth/otp", "POST", {"email": email})
    require(status, 202, response)
    status, response = request(f"{API}/v1/auth/verify", "POST", {"email": email, "code": mailbox_code(email)})
    require(status, 200, response)
    return response["accessToken"], response["userId"]


def headers(token: str):
    return {"Authorization": f"Bearer {token}"}


def call(method: str, path: str, token: str | None = None, body: dict | None = None, expected: int = 200):
    status, response = request(f"{API}{path}", method, body, token)
    return require(status, expected, response)


def rhythm(activity: str):
    day = {
        "sleepStart": "23:00", "sleepEnd": "07:00",
        "activity": activity, "activityStart": "09:00", "activityEnd": "18:00",
        "contactKnown": True, "contactStart": "20:00", "contactEnd": "22:30",
    }
    rest = {**day, "activity": "休息", "activityStart": "10:00", "activityEnd": "12:00"}
    return {"weekday": day, "rest": rest}


def main():
    suffix = uuid.uuid4().hex[:10]
    alice_token, alice_id = login(f"alice-{suffix}@example.net")
    bob_token, _ = login(f"bob-{suffix}@example.net")
    carol_token, _ = login(f"carol-{suffix}@example.net")

    call("PUT", "/v1/me/profile", alice_token, {"name": "阿青", "cityId": "beijing"})
    call("PUT", "/v1/me/profile", bob_token, {"name": "小雨", "cityId": "new-york"})
    invite = call("POST", "/v1/invites", alice_token)
    preview = call("GET", f"/v1/invites/{invite['code']}")
    assert preview["inviter"]["name"] == "阿青"
    assert "当前一条留言" in preview["scope"]
    call("POST", "/v1/invites/accept", bob_token, {"code": invite["code"]})

    call("PUT", "/v1/me/rhythm", alice_token, rhythm("上班"))
    call("PUT", "/v1/me/rhythm", bob_token, rhythm("上课"))
    call("PUT", "/v1/me/note", alice_token, {"text": "醒来给我说一声 🌙"})
    call("PUT", "/v1/me/note", bob_token, {"text": "下班一起吃饭"})

    alice = call("GET", "/v1/snapshot", alice_token)
    bob = call("GET", "/v1/snapshot", bob_token)
    carol = call("GET", "/v1/snapshot", carol_token)
    assert alice["partner"]["schedule"]["weekday"]["activity"] == "上课"
    assert alice["partner"]["note"]["text"] == "下班一起吃饭"
    assert bob["partner"]["schedule"]["weekday"]["activity"] == "上班"
    assert bob["partner"]["note"]["text"] == "醒来给我说一声 🌙"
    assert carol["partner"] is None and carol["paired"] is False
    status, _ = request(f"{API}/v1/users/{alice_id}", token=carol_token)
    assert status == 404

    call("PUT", "/v1/me/sharing", alice_token, {"enabled": False})
    assert call("GET", "/v1/snapshot", bob_token)["partner"] is None
    call("PUT", "/v1/me/sharing", alice_token, {"enabled": True})
    assert call("GET", "/v1/snapshot", bob_token)["partner"]["profile"]["name"] == "阿青"
    call("DELETE", "/v1/pairing", alice_token, expected=204)
    assert call("GET", "/v1/snapshot", bob_token)["partner"] is None
    print("PASS: A/B/C OTP login, invite preview/accept, two-way schedule/note sync, C access denial, pause/resume, and unpair revocation")


if __name__ == "__main__":
    main()
