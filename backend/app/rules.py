"""Validation shared by the API endpoints."""

import re


CITY_IDS = {
    "beijing", "shanghai", "new-york", "london", "paris", "tokyo", "sydney", "kathmandu",
}
_TIME = re.compile(r"(?:[01][0-9]|2[0-3]):[0-5][0-9]\Z")


def profile_errors(profile: dict) -> list[str]:
    errors = []
    name = profile.get("name")
    city_id = profile.get("cityId")
    if not isinstance(name, str) or not name.strip() or len(name) > 20:
        errors.append("昵称应为 1–20 个字符")
    if city_id not in CITY_IDS:
        errors.append("请选择有效城市")
    return errors


def _parse_minute(value: object) -> int | None:
    if not isinstance(value, str) or not _TIME.fullmatch(value):
        return None
    return int(value[:2]) * 60 + int(value[3:])


def _contains(minute: int, start: str, end: str) -> bool:
    left, right = _parse_minute(start), _parse_minute(end)
    if left is None or right is None or left == right:
        return False
    if left < right:
        return left <= minute < right
    return minute >= left or minute < right


def rhythm_errors(rhythm: dict) -> list[str]:
    errors = []
    activity = rhythm.get("activity")
    if not isinstance(activity, str) or not activity.strip():
        errors.append("请填写活动名称")

    keys = ["sleepStart", "sleepEnd", "activityStart", "activityEnd"]
    if rhythm.get("contactKnown") is True:
        keys += ["contactStart", "contactEnd"]
    parsed = {key: _parse_minute(rhythm.get(key)) for key in keys}
    if any(value is None for value in parsed.values()) or any(
        parsed[start] == parsed[end]
        for start, end in (("sleepStart", "sleepEnd"), ("activityStart", "activityEnd"))
    ) or (rhythm.get("contactKnown") is True and parsed["contactStart"] == parsed["contactEnd"]):
        errors.append("时间格式应为 HH:mm，起止不得相同")
    elif parsed["activityStart"] >= parsed["activityEnd"]:
        errors.append("活动时段不可跨日")
    else:
        if any(
            _contains(minute, rhythm["sleepStart"], rhythm["sleepEnd"])
            and _contains(minute, rhythm["activityStart"], rhythm["activityEnd"])
            for minute in range(1440)
        ):
            errors.append("活动不可与睡眠重叠")
        if rhythm.get("contactKnown") is True and any(
            _contains(minute, rhythm["sleepStart"], rhythm["sleepEnd"])
            and _contains(minute, rhythm["contactStart"], rhythm["contactEnd"])
            for minute in range(1440)
        ):
            errors.append("联系不可与睡眠重叠")
    if rhythm.get("contactKnown") not in (True, False):
        errors.append("联系偏好状态无效")
    return errors
