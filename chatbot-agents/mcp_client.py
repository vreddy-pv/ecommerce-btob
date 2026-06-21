"""
MCP Client Manager - Connects to Java MCP servers and exposes tools to LangGraph agents.
Uses native MCP library for SSE connections.
"""
import asyncio
from typing import Dict, List, Any
from contextlib import AsyncExitStack

from mcp import ClientSession
from mcp.client.sse import sse_client
from langchain_mcp_adapters.tools import load_mcp_tools
from langchain_core.tools import Tool

from dotenv import load_dotenv
load_dotenv()


class McpClientManager:
    """Manages connections to multiple MCP servers and provides tools to agents."""

    def __init__(self):
        self.sessions: Dict[str, ClientSession] = {}
        self.tools: Dict[str, List[Tool]] = {}
        self.exit_stack = AsyncExitStack()

    async def connect_to_server(self, server_name: str, sse_url: str):
        """
        Connect to an MCP server via SSE.

        Args:
            server_name: Name identifier for this server
            sse_url: SSE endpoint URL (e.g., http://localhost:8083/sse)
        """
        try:
            # Connect to SSE endpoint
            sse_transport = await self.exit_stack.enter_async_context(
                sse_client(url=sse_url)
            )

            # Create MCP session
            session = await self.exit_stack.enter_async_context(
                ClientSession(*sse_transport)
            )

            # Initialize the session
            await session.initialize()

            self.sessions[server_name] = session

            # Load tools from this server
            tools = await load_mcp_tools(session)
            self.tools[server_name] = tools

            print(f"Connected to MCP server: {server_name} at {sse_url}")
            print(f"  Tools available: {[t.name for t in tools]}")

        except Exception as e:
            print(f"Failed to connect to {server_name} at {sse_url}: {e}")
            import traceback
            traceback.print_exc()

    async def connect_to_servers(self, server_configs: Dict[str, Dict]):
        """
        Connect to multiple MCP servers.

        Args:
            server_configs: Dict of server_name -> config with 'url' key
        """
        for server_name, config in server_configs.items():
            await self.connect_to_server(server_name, config["url"])

    def get_tools_for_server(self, server_name: str) -> List[Tool]:
        """Get tools from a specific MCP server."""
        return self.tools.get(server_name, [])

    def get_all_tools(self) -> List[Tool]:
        """Get tools from all connected servers."""
        all_tools = []
        for tools in self.tools.values():
            all_tools.extend(tools)
        return all_tools

    async def close(self):
        """Close all MCP connections."""
        await self.exit_stack.aclose()


# Global instance
mcp_manager = McpClientManager()
