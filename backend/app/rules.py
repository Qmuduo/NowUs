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
    if rhythm.get("blocks"):
        return _routine_block_errors(rhythm)

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


def _routine_block_errors(rhythm: dict) -> list[str]:
    errors = []
    blocks = rhythm.get("blocks")
    if not isinstance(blocks, list) or not 1 <= len(blocks) <= 20:
        return ["日常时段列表无效"]
    if any(not isinstance(block, dict) for block in blocks):
        return ["日常时段列表无效"]

    ids = [block.get("id") for block in blocks]
    if (
        any(not isinstance(block_id, str) or not block_id for block_id in ids)
        or len(set(ids)) != len(ids)
        or ids.count("sleep") != 1
    ):
        errors.append("日常时段列表无效")
    if any(
        not isinstance(block.get("label"), str)
        or not block["label"].strip()
        or len(block["label"]) > 20
        for block in blocks
    ):
        errors.append("时段名称应为 1–20 个字符")

    parsed = [(block, _parse_minute(block.get("start")), _parse_minute(block.get("end"))) for block in blocks]
    invalid_times = any(
        start is None or end is None or start == end or (block.get("id") != "sleep" and start >= end)
        for block, start, end in parsed
    )
    if invalid_times:
        errors.append("时段时间格式无效，睡觉以外的时段不可跨日")
    elif not any(error == "日常时段列表无效" for error in errors):
        for index, (left, _, _) in enumerate(parsed):
            if any(
                _contains(minute, left["start"], left["end"])
                and _contains(minute, right["start"], right["end"])
                for right, _, _ in parsed[index + 1 :]
                for minute in range(1440)
            ):
                errors.append("日常时段不能重叠")
                break

    contact_known = rhythm.get("contactKnown")
    if contact_known not in (True, False):
        errors.append("联系偏好状态无效")
    elif contact_known:
        contact_start = _parse_minute(rhythm.get("contactStart"))
        contact_end = _parse_minute(rhythm.get("contactEnd"))
        if contact_start is None or contact_end is None or contact_start == contact_end:
            errors.append("时间格式应为 HH:mm，起止不得相同")
        else:
            sleep = next((block for block in blocks if block.get("id") == "sleep"), None)
            if sleep and any(
                _contains(minute, sleep["start"], sleep["end"])
                and _contains(minute, rhythm["contactStart"], rhythm["contactEnd"])
                for minute in range(1440)
            ):
                errors.append("联系不可与睡眠重叠")
    return list(dict.fromkeys(errors))
