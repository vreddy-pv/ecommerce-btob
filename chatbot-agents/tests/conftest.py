import pytest
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from dotenv import load_dotenv
load_dotenv()


@pytest.fixture
def valid_token():
    import base64
    import hmac
    import hashlib
    import json
    import time

    secret = base64.b64decode("c2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seS1ub3QtaW4tcHJvZA==")

    header = base64.urlsafe_b64encode(json.dumps({"alg": "HS256", "typ": "JWT"}).encode()).rstrip(b"=").decode()
    payload_dict = {
        "sub": "test-user",
        "accountId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "exp": int(time.time()) + 3600,
        "iat": int(time.time())
    }
    payload = base64.urlsafe_b64encode(json.dumps(payload_dict).encode()).rstrip(b"=").decode()

    signing_input = f"{header}.{payload}".encode("utf-8")
    signature = base64.urlsafe_b64encode(
        hmac.new(secret, signing_input, hashlib.sha256).digest()
    ).rstrip(b"=").decode()

    return f"{header}.{payload}.{signature}"


@pytest.fixture
def expired_token():
    import base64
    import hmac
    import hashlib
    import json
    import time

    secret = base64.b64decode("c2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seS1ub3QtaW4tcHJvZA==")

    header = base64.urlsafe_b64encode(json.dumps({"alg": "HS256", "typ": "JWT"}).encode()).rstrip(b"=").decode()
    payload_dict = {
        "sub": "test-user",
        "accountId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "exp": int(time.time()) - 3600,
        "iat": int(time.time()) - 7200
    }
    payload = base64.urlsafe_b64encode(json.dumps(payload_dict).encode()).rstrip(b"=").decode()

    signing_input = f"{header}.{payload}".encode("utf-8")
    signature = base64.urlsafe_b64encode(
        hmac.new(secret, signing_input, hashlib.sha256).digest()
    ).rstrip(b"=").decode()

    return f"{header}.{payload}.{signature}"


@pytest.fixture
def bad_signature_token():
    import base64
    import json
    import time

    header = base64.urlsafe_b64encode(json.dumps({"alg": "HS256", "typ": "JWT"}).encode()).rstrip(b"=").decode()
    payload_dict = {
        "sub": "test-user",
        "accountId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "exp": int(time.time()) + 3600
    }
    payload = base64.urlsafe_b64encode(json.dumps(payload_dict).encode()).rstrip(b"=").decode()

    return f"{header}.{payload}.bW9kaWZpZWQtc2lnbmF0dXJl"
