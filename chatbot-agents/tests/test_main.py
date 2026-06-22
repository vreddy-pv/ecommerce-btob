import pytest
from httpx import AsyncClient, ASGITransport
from unittest.mock import patch, MagicMock

from main import app


@pytest.fixture
def client():
    transport = ASGITransport(app=app)
    return AsyncClient(transport=transport, base_url="http://test")


@pytest.mark.asyncio
async def test_health_endpoint(client):
    response = await client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert "status" in data
    assert "agents" in data
    assert "mcp_servers" in data


@pytest.mark.asyncio
async def test_agents_endpoint(client):
    response = await client.get("/agents")
    assert response.status_code == 200
    data = response.json()
    assert "agents" in data
    assert "orders" in data["agents"]
    assert "catalog" in data["agents"]


@pytest.mark.asyncio
async def test_chat_without_auth(client):
    response = await client.post("/chat", json={"messages": [{"role": "user", "content": "hello"}]})
    assert response.status_code == 401
    data = response.json()
    assert "detail" in data


@pytest.mark.asyncio
async def test_chat_without_valid_token(client):
    response = await client.post(
        "/chat",
        json={"messages": [{"role": "user", "content": "hello"}]},
        headers={"Authorization": "Bearer invalidtoken"}
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_chat_send_alias_no_auth(client):
    response = await client.post("/chat/send", json={"messages": [{"role": "user", "content": "hello"}]})
    assert response.status_code == 401
