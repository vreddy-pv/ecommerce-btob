import pytest
from unittest.mock import patch, MagicMock, AsyncMock


class TestMcpClientManager:
    @pytest.mark.asyncio
    async def test_manager_initialization(self):
        from mcp_client import McpClientManager
        manager = McpClientManager()
        assert manager.client is None
        assert manager.tools == {}
        await manager.close()

    @pytest.mark.asyncio
    async def test_connect_and_get_tools(self):
        from mcp_client import McpClientManager

        mock_tool = MagicMock()
        mock_tool.name = "check_order_status"

        mock_client_instance = AsyncMock()
        mock_client_instance.get_tools.return_value = [mock_tool]

        with patch("mcp_client.MultiServerMCPClient", return_value=mock_client_instance):

            manager = McpClientManager()
            server_configs = {
                "order-service": {"url": "http://localhost:8083/sse"}
            }
            await manager.connect_to_servers(server_configs)

            assert manager.client is mock_client_instance
            mock_client_instance.get_tools.assert_called_once()

    def test_get_tools_for_unknown_server(self):
        from mcp_client import McpClientManager
        manager = McpClientManager()
        tools = manager.get_tools_for_server("nonexistent")
        assert tools == []

    def test_get_all_tools_empty(self):
        from mcp_client import McpClientManager
        manager = McpClientManager()
        all_tools = manager.get_all_tools()
        assert all_tools == []

    @pytest.mark.asyncio
    async def test_get_all_tools_aggregates(self):
        from mcp_client import McpClientManager
        manager = McpClientManager()

        mock_tool_a1 = MagicMock()
        mock_tool_a1.name = "check_order_status"
        mock_tool_a2 = MagicMock()
        mock_tool_a2.name = "create_b2b_order"
        mock_tools_a = [mock_tool_a1, mock_tool_a2]

        mock_tool_b1 = MagicMock()
        mock_tool_b1.name = "search_products"
        mock_tools_b = [mock_tool_b1]

        manager.tools["order-service"] = mock_tools_a
        manager.tools["catalog-service"] = mock_tools_b

        all_tools = manager.get_all_tools()
        assert len(all_tools) == 3

    @pytest.mark.asyncio
    async def test_get_tools_for_server_returns_correct_tools(self):
        from mcp_client import McpClientManager
        manager = McpClientManager()

        mock_tool = MagicMock()
        mock_tool.name = "order_tool"
        manager.tools["order-service"] = [mock_tool]

        tools = manager.get_tools_for_server("order-service")
        assert len(tools) == 1
        assert tools[0].name == "order_tool"
        await manager.close()

    @pytest.mark.asyncio
    async def test_connect_handles_exception(self):
        from mcp_client import McpClientManager

        with patch("mcp_client.MultiServerMCPClient", side_effect=Exception("Connection failed")):
            manager = McpClientManager()
            server_configs = {"order-service": {"url": "http://localhost:8083/sse"}}
            await manager.connect_to_servers(server_configs)

            assert manager.tools == {}
            await manager.close()
