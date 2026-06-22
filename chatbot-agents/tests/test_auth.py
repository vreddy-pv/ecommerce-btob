import pytest
from auth import decode_jwt, get_account_id_from_token


class TestDecodeJwt:
    def test_valid_token(self, valid_token):
        payload = decode_jwt(valid_token)
        assert payload is not None
        assert payload["sub"] == "test-user"
        assert payload["accountId"] == "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

    def test_expired_token(self, expired_token):
        payload = decode_jwt(expired_token)
        assert payload is None

    def test_bad_signature(self, bad_signature_token):
        payload = decode_jwt(bad_signature_token)
        assert payload is None

    def test_malformed_token(self):
        assert decode_jwt("not-a-token") is None
        assert decode_jwt("only.two") is None
        assert decode_jwt("") is None

    def test_invalid_base64(self):
        assert decode_jwt("header.payload.signature") is None


class TestGetAccountIdFromToken:
    def test_returns_account_id(self, valid_token):
        account_id = get_account_id_from_token(valid_token)
        assert account_id == "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

    def test_returns_none_for_expired(self, expired_token):
        account_id = get_account_id_from_token(expired_token)
        assert account_id is None

    def test_falls_back_to_sub(self, valid_token):
        import base64
        import hmac
        import hashlib
        import json
        import time

        secret = base64.b64decode("c2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seS1ub3QtaW4tcHJvZA==")
        header = base64.urlsafe_b64encode(json.dumps({"alg": "HS256", "typ": "JWT"}).encode()).rstrip(b"=").decode()
        payload_dict = {
            "sub": "fallback-account-id",
            "exp": int(time.time()) + 3600
        }
        payload = base64.urlsafe_b64encode(json.dumps(payload_dict).encode()).rstrip(b"=").decode()
        signing_input = f"{header}.{payload}".encode()
        signature = base64.urlsafe_b64encode(hmac.new(secret, signing_input, hashlib.sha256).digest()).rstrip(b"=").decode()
        token = f"{header}.{payload}.{signature}"

        account_id = get_account_id_from_token(token)
        assert account_id == "fallback-account-id"
