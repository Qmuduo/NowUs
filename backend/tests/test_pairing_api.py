from concurrent.futures import ThreadPoolExecutor
from datetime import timedelta

from .helpers import bearer, create_user, save_profile


def test_invitation_preview_shows_limited_scope_and_acceptance_is_explicit(api):
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    save_profile(client, alice, "阿青", "beijing")
    invite = client.post("/v1/invites", headers=bearer(alice)).json()
    preview = client.get(f"/v1/invites/{invite['code']}")
    assert preview.status_code == 200
    assert preview.json()["inviter"] == {"name": "阿青", "cityId": "beijing", "cityName": "北京"}
    assert preview.json()["scope"] == ["昵称与城市", "通常作息与联系偏好", "临时联系意愿", "当前一条留言"]
    assert client.get("/v1/snapshot", headers=bearer(bob)).json()["paired"] is False
    accepted = client.post("/v1/invites/accept", headers=bearer(bob), json={"code": invite["code"]})
    assert accepted.status_code == 200
    bob_snapshot = client.get("/v1/snapshot", headers=bearer(bob)).json()
    assert bob_snapshot["partner"]["profile"]["name"] == "阿青"
    assert bob_snapshot["partner"]["schedule"] is None


def test_inviter_cannot_accept_own_invitation_and_expired_invitation_is_rejected(api):
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    save_profile(client, alice, "阿青")
    invite = client.post("/v1/invites", headers=bearer(alice)).json()
    self_accept = client.post("/v1/invites/accept", headers=bearer(alice), json={"code": invite["code"]})
    assert self_accept.status_code == 409
    assert self_accept.json()["detail"]["code"] == "invite_self_accept"

    now = client.app.state.clock()
    client.app.state.clock = lambda: now + timedelta(hours=24, seconds=1)
    expired = client.get(f"/v1/invites/{invite['code']}")
    assert expired.status_code == 410
    assert expired.json()["detail"]["code"] == "invite_expired"


def test_revoke_and_rebuild_invalidates_previous_code(api):
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    save_profile(client, alice, "阿青")
    old = client.post("/v1/invites", headers=bearer(alice)).json()["code"]
    assert client.delete("/v1/me/invitation", headers=bearer(alice)).status_code == 204
    new = client.post("/v1/invites", headers=bearer(alice)).json()["code"]
    assert old != new
    assert client.post("/v1/invites/accept", headers=bearer(bob), json={"code": old}).status_code == 410
    assert client.post("/v1/invites/accept", headers=bearer(bob), json={"code": new}).status_code == 200


def test_simultaneous_acceptance_of_one_invite_has_exactly_one_winner(api):
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    carol, _ = create_user(client, mailer, "carol@example.com")
    save_profile(client, alice, "阿青")
    code = client.post("/v1/invites", headers=bearer(alice)).json()["code"]

    def accept(token):
        return client.post("/v1/invites/accept", headers=bearer(token), json={"code": code}).status_code

    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(accept, [bob, carol]))
    assert sorted(results) == [200, 410]
    assert sum(client.get("/v1/snapshot", headers=bearer(token)).json()["paired"] for token in (alice, bob, carol)) == 2


def test_paired_account_cannot_create_or_accept_another_pair(api):
    client, mailer = api
    alice, _ = create_user(client, mailer, "alice@example.com")
    bob, _ = create_user(client, mailer, "bob@example.com")
    carol, _ = create_user(client, mailer, "carol@example.com")
    for token, name in ((alice, "阿青"), (carol, "小雨")):
        save_profile(client, token, name)
    invite_a = client.post("/v1/invites", headers=bearer(alice)).json()["code"]
    assert client.post("/v1/invites/accept", headers=bearer(bob), json={"code": invite_a}).status_code == 200
    assert client.post("/v1/invites", headers=bearer(alice)).status_code == 409
    invite_c = client.post("/v1/invites", headers=bearer(carol)).json()["code"]
    cannot_accept = client.post("/v1/invites/accept", headers=bearer(bob), json={"code": invite_c})
    assert cannot_accept.status_code == 409
    assert client.get("/v1/snapshot", headers=bearer(bob)).json()["partner"]["profile"]["name"] == "阿青"
