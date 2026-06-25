"""
Catalog Agent - Specialized agent for product catalog operations.
Handles: search products, get product details, tier pricing.
"""
from langchain_groq import ChatGroq
from langgraph.prebuilt import create_react_agent
from typing import List, Any

from dotenv import load_dotenv
load_dotenv()


# System prompt for Catalog Agent
CATALOG_AGENT_PROMPT = """
You are a smart B2B Auto Parts Catalog Assistant. You understand natural language and help customers find the right parts.

Available tools:
- search_products(query): Search products by name, keyword, or category. Use this for "find", "search", "do you have", "show me" queries.
- get_product_by_sku(sku): Get full details for a specific product by SKU.
- get_low_stock_items(): Show products running low on inventory.
- check_stock(sku): Check current stock level for a product.

How to handle user requests:
- "find brake pads" / "search for brake pads" / "do you have brake pads" -> search_products("brake pads")
- "show me oil filters" -> search_products("oil filters")
- "BRK-001 details" / "tell me about BRK-001" -> get_product_by_sku("BRK-001")
- "what's in stock?" / "show available products" -> search_products with a broad query
- "low stock items" / "what's running low?" -> get_low_stock_items()

Rules:
- Always search first when the user describes what they need
- Show SKU, name, price, and inventory in responses
- Include tier pricing when available
- Be conversational and helpful
- Suggest related products when relevant
"""


def create_catalog_agent(tools: List) -> Any:
    """
    Create the Catalog Agent with MCP tools.

    Args:
        tools: List of MCP tools from catalog-service

    Returns:
        Compiled LangGraph agent
    """
    llm = ChatGroq(
        model="llama-3.3-70b-versatile",
        temperature=0.1,
        max_tokens=1024
    )

    agent = create_react_agent(
        model=llm,
        tools=tools,
        prompt=CATALOG_AGENT_PROMPT
    )

    return agent
