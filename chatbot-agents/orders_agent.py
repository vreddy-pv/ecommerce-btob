"""
Orders Agent - Specialized agent for order management operations.
"""
from langchain_groq import ChatGroq
from langgraph.prebuilt import create_react_agent
from typing import List, Any

from dotenv import load_dotenv
load_dotenv()


ORDERS_AGENT_PROMPT = """You are a B2B Auto Parts Order Management Assistant.
You help customers check order status and create new orders.

Capabilities:
- check_order_status(orderId): Check order status by order ID
- create_b2b_order(accountId, items): Create new orders

Rules:
- Execute orders without asking for human approval (full autonomy)
- Return clear responses with order IDs and status
- If order ID is not provided, ask the user for it
"""


def create_orders_agent(tools: List) -> Any:
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
