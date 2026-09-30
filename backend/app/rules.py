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


def _intervals_overlap(first_start: int, first_end: int, second_start: int, second_end: int) -> bool:
    if any(value < 0 or value >= 1440 for value in (first_start, first_end, second_start, second_end)):
        return False
    if first_start == first_end or second_start == second_end:
        return False

    def pieces(start: int, end: int) -> tuple[tuple[int, int], ...]:
        return ((start, end),) if start < end else ((start, 1440), (0, end))

    return any(
        first_left < second_right and second_left < first_right
        for first_left, first_right in pieces(first_start, first_end)
        for second_left, second_right in pieces(second_start, second_end)
    )


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
        if _intervals_overlap(
            parsed["sleepStart"], parsed["sleepEnd"],
            parsed["activityStart"], parsed["activityEnd"],
        ):
            errors.append("活动不可与睡眠重叠")
        if rhythm.get("contactKnown") is True and _intervals_overlap(
            parsed["sleepStart"], parsed["sleepEnd"],
            parsed["contactStart"], parsed["contactEnd"],
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
        for index, (_, left_start, left_end) in enumerate(parsed):
            if any(
                _intervals_overlap(left_start, left_end, right_start, right_end)
                for _, right_start, right_end in parsed[index + 1 :]
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
            sleep = next((entry for entry in parsed if entry[0].get("id") == "sleep"), None)
            if sleep and not invalid_times and _intervals_overlap(
                sleep[1], sleep[2], contact_start, contact_end,
            ):
                errors.append("联系不可与睡眠重叠")
    return list(dict.fromkeys(errors))
