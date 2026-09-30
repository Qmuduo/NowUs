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


def sample_segmented_rhythm(activity: str = "上班", template: str = "上班族") -> dict:
    is_student = template == "学生"
    main_label = "上午上课" if is_student else "上午上班"
    afternoon_label = "下午上课" if is_student else "下午上班"
    weekday_blocks = [
        {"id": "sleep", "label": "睡觉", "start": "23:00", "end": "07:00", "category": "SLEEP"},
        {"id": "preparation", "label": "起床准备", "start": "07:00", "end": "07:30", "category": "PREPARATION"},
        {"id": "breakfast", "label": "早餐", "start": "07:30", "end": "08:00", "category": "MEAL"},
        {"id": "commute", "label": "通勤", "start": "08:00", "end": "09:00", "category": "COMMUTE"},
        {"id": "morning", "label": main_label, "start": "09:00", "end": "12:00", "category": "STUDY_WORK"},
        {"id": "lunch", "label": "午餐", "start": "12:00", "end": "13:00", "category": "MEAL"},
        {"id": "nap", "label": "午休", "start": "13:00", "end": "13:30", "category": "REST"},
        {"id": "afternoon", "label": afternoon_label, "start": "13:30", "end": "17:30", "category": "STUDY_WORK"},
        {"id": "commute-home", "label": "返程通勤", "start": "17:30", "end": "18:00", "category": "COMMUTE"},
        {"id": "dinner", "label": "晚餐", "start": "18:00", "end": "19:00", "category": "MEAL"},
    ]
    rest_blocks = [
        {"id": "sleep", "label": "睡觉", "start": "23:30", "end": "08:30", "category": "SLEEP"},
        {"id": "preparation", "label": "起床准备", "start": "08:30", "end": "08:45", "category": "PREPARATION"},
        {"id": "breakfast", "label": "早餐", "start": "08:45", "end": "09:15", "category": "MEAL"},
        {"id": "lunch", "label": "午餐", "start": "12:30", "end": "13:30", "category": "MEAL"},
        {"id": "free", "label": "自由安排", "start": "14:00", "end": "18:00", "category": "OTHER"},
        {"id": "dinner", "label": "晚餐", "start": "18:30", "end": "19:30", "category": "MEAL"},
        {"id": "evening", "label": "晚间休息", "start": "19:30", "end": "22:30", "category": "REST"},
    ]
    return {
        "templateId": template,
        "weekday": {
            "sleepStart": "23:00", "sleepEnd": "07:00", "activity": activity,
            "activityStart": "09:00", "activityEnd": "12:00", "contactKnown": True,
            "contactStart": "20:00", "contactEnd": "22:00", "blocks": weekday_blocks,
        },
        "rest": {
            "sleepStart": "23:30", "sleepEnd": "08:30", "activity": "休息",
            "activityStart": "10:00", "activityEnd": "12:00", "contactKnown": True,
            "contactStart": "20:00", "contactEnd": "22:00", "blocks": rest_blocks,
        },
    }
