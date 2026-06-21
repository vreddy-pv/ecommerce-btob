"""
MCP Client Manager - Connects to Java MCP servers and exposes tools to LangGraph agents.
"""
import asyncio
from contextlib import AsyncExitStack
from typing import Dict, List

from langchain_mcp_adapters.client import MultiServerMCPClient
from mcp import ClientSession, StdioServerParameters
from mcp.client.sse import sse_client


class McpClientManager:
    """Manages connections to multiple MCP servers and provides tools to agents."""

    def __init__(self):
        self.clients: Dict[str, MultiServerMCPClient] = {}
        self.tools: Dict[str, List] = {}

    async def connect_to_servers(self, server_configs: Dict[str, Dict]):
        """
        Connect to multiple MCP servers.

        Args:
            server_configs: Dict of server_name -> config with 'url' key
        """
        for server_name, config in server_configs.items():
            try:
                client = MultiServerMCPClient(
                    connections={
                        server_name: {
                            "transport": "sse",
                            "url": config["url"],
                        }
                    }
                )
                await client.__aenter__()
                self.clients[server_name] = client
                self.tools[server_name] = client.get_tools()
                print(f"Connected to MCP server: {server_name} at {config['url']}")
            except Exception as e:
                print(f"Failed to connect to {server_name}: {e}")

    def get_tools_for_server(self, server_name: str) -> List:
        """Get tools from a specific MCP server."""
        return self.tools.get(server_name, [])

    def get_all_tools(self) -> List:
        """Get tools from all connected servers."""
        all_tools = []
        for tools in self.tools.values():
            all_tools.extend(tools)
        return all_tools

    async def close(self):
        """Close all MCP connections."""
        for client in self.clients.values():
            await client.__aexit__(None, None, None)


# Global instance
mcp_manager = McpClientManager()
