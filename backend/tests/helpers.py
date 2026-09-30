def bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def create_user(client, mailer, email: str) -> tuple[str, str]:
    requested = client.post("/v1/auth/otp", json={"email": email})
    assert requested.status_code == 202, requested.text
    code = mailer.latest(email.strip().lower())
    verified = client.post("/v1/auth/verify", json={"email": email, "code": code})
    assert verified.status_code == 200, verified.text
    body = verified.json()
    return body["accessToken"], body["userId"]


def save_profile(client, token: str, name: str, city_id: str = "beijing") -> None:
    response = client.put("/v1/me/profile", headers=bearer(token), json={"name": name, "cityId": city_id})
    assert response.status_code == 200, response.text


def sample_rhythm(activity: str = "上班") -> dict:
    rhythm = {
        "sleepStart": "23:00", "sleepEnd": "07:00",
        "activity": activity, "activityStart": "09:00", "activityEnd": "18:00",
        "contactKnown": True, "contactStart": "20:00", "contactEnd": "22:30",
    }
    return {"weekday": rhythm, "rest": {**rhythm, "activity": "休息", "activityStart": "10:00", "activityEnd": "12:00"}}
