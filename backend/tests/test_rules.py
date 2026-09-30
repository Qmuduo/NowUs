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


def test_routine_blocks_validate_all_local_activities_and_overlaps():
    rhythm = {
        "sleepStart": "23:00", "sleepEnd": "07:00",
        "activity": "上课", "activityStart": "08:30", "activityEnd": "17:00",
        "contactKnown": False,
        "blocks": [
            {"id": "sleep", "label": "睡觉", "start": "23:00", "end": "07:00"},
            {"id": "breakfast", "label": "早餐", "start": "07:00", "end": "07:30"},
            {"id": "commute", "label": "通勤", "start": "07:30", "end": "08:30"},
            {"id": "morning", "label": "上午上课", "start": "08:30", "end": "12:00"},
            {"id": "lunch", "label": "午餐", "start": "12:00", "end": "12:45"},
        ],
    }
    assert rules.rhythm_errors(rhythm) == []
    overlap = {**rhythm, "blocks": [*rhythm["blocks"], {"id": "nap", "label": "午休", "start": "12:30", "end": "13:00"}]}
    assert "日常时段不能重叠" in rules.rhythm_errors(overlap)
    crosses_midnight = {**rhythm, "blocks": [*rhythm["blocks"], {"id": "study", "label": "晚间学习", "start": "22:00", "end": "01:00"}]}
    assert "时段时间格式无效，睡觉以外的时段不可跨日" in rules.rhythm_errors(crosses_midnight)
