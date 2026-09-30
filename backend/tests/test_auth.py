from backend.app import auth


def test_generate_otp_uses_a_cryptographic_digit_for_each_position(monkeypatch):
    values = iter([0, 0, 1, 2, 3, 4])
    monkeypatch.setattr(auth.secrets, "randbelow", lambda upper: next(values))
    assert auth.generate_otp() == "001234"


def test_one_way_digest_is_keyed_and_does_not_store_the_otp():
    digest = auth.digest_secret("123456", "a" * 32)
    assert digest != "123456"
    assert digest != auth.digest_secret("123456", "b" * 32)
    assert digest == auth.digest_secret("123456", "a" * 32)
