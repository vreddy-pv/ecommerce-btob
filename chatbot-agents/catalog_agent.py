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
You are a B2B Auto Parts Catalog Assistant. You help customers find and learn about auto parts.

Your capabilities:
- Search for products by name or SKU
- Get detailed product information by SKU
- Show tier pricing for B2B customers (BRONZE, SILVER, GOLD, PLATINUM)
- Filter products by category

Rules:
- Always show SKU, name, price, and inventory in responses
- Include tier pricing when available
- If multiple products match, show top results with key details
- For specific product queries, show full details including description
- Help users understand which products fit their needs

When responding:
- Be helpful and informative
- Show prices clearly with currency
- Mention inventory availability
- Suggest related products when relevant
- For B2B customers, highlight tier discounts
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
