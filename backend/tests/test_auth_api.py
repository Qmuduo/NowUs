from datetime import timedelta

from .helpers import create_user


def test_otp_is_sent_normalized_expires_and_creates_a_restorable_session(api):
    client, mailer = api
    requested = client.post("/v1/auth/otp", json={"email": "  ALICE@example.com "})
    assert requested.status_code == 202
    assert requested.json()["retryAfterSeconds"] == 60
    code = mailer.latest("alice@example.com")
    assert len(code) == 6 and code.isdigit()

    verified = client.post("/v1/auth/verify", json={"email": "alice@example.com", "code": code})
    assert verified.status_code == 200
    token = verified.json()["accessToken"]
    assert client.get("/v1/snapshot", headers={"Authorization": f"Bearer {token}"}).status_code == 200
    assert client.post("/v1/auth/verify", json={"email": "alice@example.com", "code": code}).status_code == 400


def test_wrong_codes_are_limited_and_do_not_consume_the_correct_code_immediately(api):
    client, mailer = api
    email = "wrong@example.com"
    client.post("/v1/auth/otp", json={"email": email})
    valid = mailer.latest(email)
    wrong = "999999" if valid != "999999" else "888888"
    for attempt in range(5):
        response = client.post("/v1/auth/verify", json={"email": email, "code": wrong})
        assert response.status_code == (429 if attempt == 4 else 400)
        assert response.json()["detail"]["code"] == ("otp_attempts_exceeded" if attempt == 4 else "otp_invalid")
    blocked = client.post("/v1/auth/verify", json={"email": email, "code": valid})
    assert blocked.status_code == 429
    assert blocked.json()["detail"]["code"] == "otp_attempts_exceeded"


def test_expired_code_has_a_distinct_error(api):
    client, mailer = api
    email = "expired@example.com"
    client.post("/v1/auth/otp", json={"email": email})
    code = mailer.latest(email)
    now = client.app.state.clock()
    client.app.state.clock = lambda: now + timedelta(minutes=11)
    response = client.post("/v1/auth/verify", json={"email": email, "code": code})
    assert response.status_code == 410
    assert response.json()["detail"]["code"] == "otp_expired"


def test_resend_cooldown_and_per_email_hourly_limit_are_enforced_by_server(api):
    client, mailer = api
    email = "limits@example.com"
    assert client.post("/v1/auth/otp", json={"email": email}).status_code == 202
    cooldown = client.post("/v1/auth/otp", json={"email": email})
    assert cooldown.status_code == 429
    assert cooldown.json()["detail"]["code"] == "otp_resend_wait"
    now = client.app.state.clock()
    for index in range(1, 5):
        client.app.state.clock = lambda index=index: now + timedelta(seconds=60 * index)
        assert client.post("/v1/auth/otp", json={"email": email}).status_code == 202
    client.app.state.clock = lambda: now + timedelta(minutes=10)
    limited = client.post("/v1/auth/otp", json={"email": email})
    assert limited.status_code == 429
    assert limited.json()["detail"]["code"] == "otp_hourly_limit"


def test_logout_revokes_session_and_unrecognized_sessions_cannot_read(api):
    client, mailer = api
    token, _ = create_user(client, mailer, "session@example.com")
    headers = {"Authorization": f"Bearer {token}"}
    assert client.delete("/v1/auth/session", headers=headers).status_code == 204
    assert client.get("/v1/snapshot", headers=headers).status_code == 401
    assert client.get("/v1/snapshot", headers={"Authorization": "Bearer guessed"}).status_code == 401
