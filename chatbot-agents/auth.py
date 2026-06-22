"""
JWT Authentication module for chatbot-agents.
Validates JWT tokens from account-service.
"""
import os
import base64
import hmac
import hashlib
import json
from datetime import datetime, timezone
from typing import Optional, Dict
from fastapi import HTTPException, Header
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

# JWT secret (same as account-service)
JWT_SECRET = os.getenv("JWT_SECRET", "c2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seS1ub3QtaW4tcHJvZA==")

security = HTTPBearer(auto_error=False)


def decode_jwt(token: str) -> Optional[Dict]:
    """
    Decode and validate JWT token.
    Returns payload if valid, None otherwise.
    """
    try:
        # Split token
        parts = token.split(".")
        if len(parts) != 3:
            return None

        header_b64, payload_b64, signature_b64 = parts

        # Verify signature
        secret_bytes = base64.b64decode(JWT_SECRET)
        signing_input = f"{header_b64}.{payload_b64}".encode("utf-8")
        expected_signature = hmac.new(
            secret_bytes,
            signing_input,
            hashlib.sha256
        ).digest()

        # Decode signature from base64url
        signature = base64.urlsafe_b64decode(signature_b64 + "==")

        if not hmac.compare_digest(expected_signature, signature):
            return None

        # Decode payload
        payload_bytes = base64.urlsafe_b64decode(payload_b64 + "==")
        payload = json.loads(payload_bytes)

        # Check expiration
        if "exp" in payload:
            exp = datetime.fromtimestamp(payload["exp"], tz=timezone.utc)
            if datetime.now(timezone.utc) > exp:
                return None

        return payload

    except Exception as e:
        print(f"JWT decode error: {e}")
        return None


async def get_current_user(authorization: Optional[str] = Header(None)) -> Optional[HTTPAuthorizationCredentials]:
    """
    Extract and validate JWT from Authorization header.
    Returns credentials if valid, raises HTTPException otherwise.
    """
    if not authorization:
        raise HTTPException(
            status_code=401,
            detail="Missing Authorization header"
        )

    # Extract token from "Bearer <token>"
    parts = authorization.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=401,
            detail="Invalid Authorization header format"
        )

    token = parts[1]
    payload = decode_jwt(token)

    if not payload:
        raise HTTPException(
            status_code=401,
            detail="Invalid or expired token"
        )

    return HTTPAuthorizationCredentials(scheme="Bearer", credentials=token)


def get_account_id_from_token(token: str) -> Optional[str]:
    """
    Extract accountId from JWT token.
    Returns accountId if valid, None otherwise.
    """
    payload = decode_jwt(token)
    if not payload:
        return None

    # accountId is in the payload (set during login)
    return payload.get("accountId") or payload.get("sub")
