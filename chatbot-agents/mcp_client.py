"""
MCP Client Manager - Connects to Java MCP servers and exposes tools to LangGraph agents.
Uses MultiServerMCPClient from langchain-mcp-adapters.
"""
import asyncio
from typing import Dict, List, Any
from contextlib import AsyncExitStack

from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_core.tools import Tool

from dotenv import load_dotenv
load_dotenv()


class McpClientManager:
    """Manages connections to multiple MCP servers and provides tools to agents."""

    def __init__(self):
        self.client: MultiServerMCPClient | None = None
        self.tools: Dict[str, List[Tool]] = {}

    async def connect_to_servers(self, server_configs: Dict[str, Dict]):
        """
        Connect to multiple MCP servers.

        Args:
            server_configs: Dict of server_name -> config with 'url' key
        """
        try:
            # Build server config for MultiServerMCPClient
            servers = {}
            for server_name, config in server_configs.items():
                servers[server_name] = {
                    "url": config["url"],
                    "transport": "sse",
                }

            self.client = MultiServerMCPClient(servers)

            # Get all tools from all servers
            all_tools = await self.client.get_tools()

            # Group tools by server name using tool name prefix
            for tool in all_tools:
                # Tools from langchain-mcp-adapters are prefixed with server_name
                for server_name in server_configs.keys():
                    prefix = f"{server_name}__"
                    if hasattr(tool, 'name') and tool.name.startswith(prefix):
                        clean_name = tool.name[len(prefix):]
                        if server_name not in self.tools:
                            self.tools[server_name] = []
                        self.tools[server_name].append(tool)
                    elif not any(t.name.startswith(s + "__") for s in server_configs.keys() for t in [tool]):
                        # No prefix means it came from the first server or single server
                        pass

            # If no prefixed tools found, assign all tools to all servers
            if not self.tools:
                for server_name in server_configs.keys():
                    self.tools[server_name] = list(all_tools)

            for server_name, tools in self.tools.items():
                print(f"Connected to MCP server: {server_name}")
                print(f"  Tools available: {[t.name for t in tools]}")

        except Exception as e:
            print(f"Failed to connect to MCP servers: {e}")
            import traceback
            traceback.print_exc()

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
        pass


# Global instance
mcp_manager = McpClientManager()
