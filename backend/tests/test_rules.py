from backend.app import rules


def test_profile_validation_accepts_supported_city_and_rejects_invalid_fields():
    assert rules.profile_errors({"name": "阿青", "cityId": "beijing"}) == []
    assert "昵称应为 1–20 个字符" in rules.profile_errors({"name": "  ", "cityId": "beijing"})
    assert "请选择有效城市" in rules.profile_errors({"name": "阿青", "cityId": "mars"})
    assert "昵称应为 1–20 个字符" in rules.profile_errors({"name": "木" * 21, "cityId": "beijing"})


def test_rhythm_validation_preserves_unknown_contact_and_rejects_sleep_overlap():
    rhythm = {
        "sleepStart": "23:00", "sleepEnd": "07:00",
        "activity": "上班", "activityStart": "09:00", "activityEnd": "18:00",
        "contactKnown": False, "contactStart": "xx", "contactEnd": "xx",
    }
    assert rules.rhythm_errors(rhythm) == []
    assert "活动不可与睡眠重叠" in rules.rhythm_errors({**rhythm, "activityStart": "06:00", "activityEnd": "08:00"})


def test_rhythm_validation_rejects_contact_time_inside_overnight_sleep():
    rhythm = {
        "sleepStart": "23:00", "sleepEnd": "07:00",
        "activity": "上班", "activityStart": "09:00", "activityEnd": "18:00",
        "contactKnown": True, "contactStart": "06:00", "contactEnd": "06:30",
    }
    assert "联系不可与睡眠重叠" in rules.rhythm_errors(rhythm)
