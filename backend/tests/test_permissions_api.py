from .helpers import bearer, create_user, sample_rhythm, sample_segmented_rhythm, save_profile


def test_pair_members_see_each_others_owned_schedule_and_note_but_third_user_cannot(api):
    client, mailer = api
    alice, alice_id = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    carol, _ = create_user(client, mailer, "carol@example.com")
    save_profile(client, alice, "阿青", "beijing")
    save_profile(client, bob, "小雨", "new-york")
    code = client.post("/v1/invites", headers=bearer(alice)).json()["code"]
    client.post("/v1/invites/accept", headers=bearer(bob), json={"code": code}).raise_for_status()

    alice_schedule = sample_segmented_rhythm("上班", "上班族")
    bob_schedule = sample_rhythm("上课")
    client.put("/v1/me/rhythm", headers=bearer(alice), json=alice_schedule).raise_for_status()
    client.put("/v1/me/rhythm", headers=bearer(bob), json=bob_schedule).raise_for_status()
    client.put("/v1/me/note", headers=bearer(alice), json={"text": "醒来给我说一声 🌙"}).raise_for_status()
    client.put("/v1/me/note", headers=bearer(bob), json={"text": "下班一起吃饭"}).raise_for_status()

    view_a = client.get("/v1/snapshot", headers=bearer(alice)).json()
    view_b = client.get("/v1/snapshot", headers=bearer(bob)).json()
    assert view_a["partner"]["profile"]["name"] == "小雨"
    assert view_a["partner"]["schedule"]["weekday"]["activity"] == "上课"
    assert view_a["partner"]["note"]["text"] == "下班一起吃饭"
    assert view_b["partner"]["schedule"]["weekday"]["activity"] == "上班"
    assert view_b["partner"]["schedule"]["templateId"] == "上班族"
    shared_blocks = view_b["partner"]["schedule"]["weekday"]["blocks"]
    assert [block["label"] for block in shared_blocks if block["id"] in {"breakfast", "commute", "morning", "lunch", "nap"}] == ["早餐", "通勤", "上午上班", "午餐", "午休"]
    assert next(block for block in shared_blocks if block["id"] == "morning")["category"] == "STUDY_WORK"
    assert next(block for block in shared_blocks if block["id"] == "preparation")["category"] == "PREPARATION"
    assert view_b["partner"]["note"]["text"] == "醒来给我说一声 🌙"

    carol_view = client.get("/v1/snapshot", headers=bearer(carol)).json()
    assert carol_view["partner"] is None
    assert carol_view["paired"] is False
    assert client.get(f"/v1/users/{alice_id}", headers=bearer(carol)).status_code == 404


def test_pausing_hides_shared_snapshot_and_resume_restores_it(api):
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    save_profile(client, alice, "阿青")
    save_profile(client, bob, "小雨")
    code = client.post("/v1/invites", headers=bearer(alice)).json()["code"]
    client.post("/v1/invites/accept", headers=bearer(bob), json={"code": code}).raise_for_status()
    client.put("/v1/me/rhythm", headers=bearer(alice), json=sample_rhythm()).raise_for_status()

    paused = client.put("/v1/me/sharing", headers=bearer(alice), json={"enabled": False})
    assert paused.status_code == 200
    bob_view = client.get("/v1/snapshot", headers=bearer(bob)).json()
    assert bob_view["pairStatus"] == "sharing_paused"
    assert bob_view["partner"] is None
    assert client.get("/v1/snapshot", headers=bearer(alice)).json()["me"]["schedule"] is not None

    client.put("/v1/me/sharing", headers=bearer(alice), json={"enabled": True}).raise_for_status()
    assert client.get("/v1/snapshot", headers=bearer(bob)).json()["partner"]["schedule"] is not None


def test_unpair_revokes_both_directions_without_deleting_personal_data(api):
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    save_profile(client, alice, "阿青")
    client.put("/v1/me/rhythm", headers=bearer(alice), json=sample_rhythm()).raise_for_status()
    code = client.post("/v1/invites", headers=bearer(alice)).json()["code"]
    client.post("/v1/invites/accept", headers=bearer(bob), json={"code": code}).raise_for_status()

    assert client.delete("/v1/pairing", headers=bearer(alice)).status_code == 204
    a_view = client.get("/v1/snapshot", headers=bearer(alice)).json()
    b_view = client.get("/v1/snapshot", headers=bearer(bob)).json()
    assert a_view["pairStatus"] == b_view["pairStatus"] == "not_paired"
    assert a_view["me"]["schedule"] is not None
    assert b_view["partner"] is None
