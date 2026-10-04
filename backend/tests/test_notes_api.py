from .helpers import bearer, create_user


def save(client, token, text="original"):
    response = client.put("/v1/me/note", headers=bearer(token), json={"text": text})
    response.raise_for_status()
    return response.json()["note"]


def delete(client, token, revision):
    return client.delete("/v1/me/note", headers=bearer(token), params={"expectedRevision": revision})


def restore(client, token, revision):
    return client.post("/v1/me/note/restore", headers=bearer(token), json={"expectedRevision": revision})


def test_note_delete_requires_revision_and_stale_delete_preserves_current(api):
    client, mailer = api
    token, _ = create_user(client, mailer, "alice@example.com")
    old = save(client, token)
    latest = save(client, token, "newer")
    assert latest["updatedAt"] != old["updatedAt"]
    assert client.delete("/v1/me/note", headers=bearer(token)).status_code == 422
    assert delete(client, token, old["updatedAt"]).status_code == 409
    assert client.get("/v1/snapshot", headers=bearer(token)).json()["me"]["note"] == latest


def test_deleted_revision_restores_exact_text_once_without_client_upsert(api):
    client, mailer = api
    token, _ = create_user(client, mailer, "alice@example.com")
    original = save(client, token)
    assert delete(client, token, original["updatedAt"]).status_code == 204
    assert client.get("/v1/snapshot", headers=bearer(token)).json()["me"]["note"] is None
    response = restore(client, token, original["updatedAt"])
    assert response.status_code == 200
    restored = response.json()["note"]
    assert restored["text"] == "original"
    assert restored["updatedAt"] != original["updatedAt"]
    assert restore(client, token, original["updatedAt"]).status_code == 409
    assert delete(client, token, restored["updatedAt"]).status_code == 204
    assert restore(client, token, original["updatedAt"]).status_code == 409


def test_stale_restore_cannot_overwrite_new_note_or_new_deleted_revision(api):
    client, mailer = api
    token, _ = create_user(client, mailer, "alice@example.com")
    original = save(client, token)
    assert delete(client, token, original["updatedAt"]).status_code == 204
    latest = save(client, token, "newer")
    assert restore(client, token, original["updatedAt"]).status_code == 409
    assert client.get("/v1/snapshot", headers=bearer(token)).json()["me"]["note"] == latest
    assert delete(client, token, latest["updatedAt"]).status_code == 204
    assert restore(client, token, original["updatedAt"]).status_code == 409


def test_note_revision_is_owned_by_authenticated_account(api):
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    note = save(client, alice)
    assert delete(client, bob, note["updatedAt"]).status_code == 409
    assert restore(client, bob, note["updatedAt"]).status_code == 409
    assert client.get("/v1/snapshot", headers=bearer(alice)).json()["me"]["note"] == note
    assert client.delete("/v1/me/note", params={"expectedRevision": note["updatedAt"]}).status_code == 401


def test_note_accepts_120_supplementary_codepoints_and_failed_validation_preserves_note(api):
    client, mailer = api
    token, _ = create_user(client, mailer, "alice@example.com")
    note = save(client, token, "🌙" * 120)
    for text in ["🌙" * 121, "   "]:
        assert client.put("/v1/me/note", headers=bearer(token), json={"text": text}).status_code == 422
    assert client.get("/v1/snapshot", headers=bearer(token)).json()["me"]["note"] == note


def test_partner_snapshot_hides_deleted_note_and_sharing_permission_still_applies(api):
    from .helpers import save_profile
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    save_profile(client, alice, "A")
    save_profile(client, bob, "B")
    code = client.post("/v1/invites", headers=bearer(alice)).json()["code"]
    client.post("/v1/invites/accept", headers=bearer(bob), json={"code": code}).raise_for_status()
    original = save(client, alice)
    assert delete(client, alice, original["updatedAt"]).status_code == 204
    assert client.get("/v1/snapshot", headers=bearer(bob)).json()["partner"]["note"] is None
    assert restore(client, alice, original["updatedAt"]).status_code == 200
    assert client.get("/v1/snapshot", headers=bearer(bob)).json()["partner"]["note"]["text"] == "original"
    client.put("/v1/me/sharing", headers=bearer(alice), json={"enabled": False}).raise_for_status()
    assert client.get("/v1/snapshot", headers=bearer(bob)).json()["partner"] is None
