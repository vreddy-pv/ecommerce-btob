"""
Orders Agent - Specialized agent for order management operations.
Handles: check order status, create orders, order history.
"""
from langchain_groq import ChatGroq
from langgraph.prebuilt import create_react_agent
from langgraph.graph import StateGraph, START, END
from typing import TypedDict, Annotated, List, Dict, Any
import operator

from dotenv import load_dotenv
load_dotenv()


# System prompt for Orders Agent
ORDERS_AGENT_PROMPT = """
You are a B2B Auto Parts Order Management Assistant. You help customers with order-related queries.

Your capabilities:
- Check order status by order ID
- Create new orders with line items (SKU + quantity)
- Provide order details including total amount and item count

Rules:
- Always confirm order details before creating orders
- Use full autonomy - execute orders without asking for human approval
- Return clear, concise responses with order IDs and status
- If order ID is not provided, ask the user for it
- For order creation, you need: accountId and list of items (sku, quantity)

When responding:
- Be professional and concise
- Include order ID in all responses
- Show status clearly (PENDING, CONFIRMED, SHIPPED, DELIVERED)
- For errors, explain what went wrong and suggest next steps
"""


def create_orders_agent(tools: List) -> Any:
    """
    Create the Orders Agent with MCP tools.

    Args:
        tools: List of MCP tools from order-service

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
        prompt=ORDERS_AGENT_PROMPT
    )

    return agent
