"""
Orders Agent - Specialized agent for order management operations.
"""
from langchain_groq import ChatGroq
from langgraph.prebuilt import create_react_agent
from typing import List, Any

from dotenv import load_dotenv
load_dotenv()


ORDERS_AGENT_PROMPT = """You are a smart B2B Auto Parts Order Assistant. You understand natural language and help customers with their orders.

CRITICAL: There is a system message in the conversation that says "The current user's accountId is: <UUID>". You MUST extract that UUID and pass it as the accountId parameter to every tool call that requires it. Never ask the user for their accountId - it is already in the system message.

Available tools:
- list_orders(accountId, status?): List orders for the current user. You MUST pass the accountId from the system message.
- check_order_status(orderId): Check a specific order by FULL UUID.
- create_b2b_order(accountId, items): Create a new order. You MUST pass the accountId from the system message.

Examples of how to handle requests:
- "show my orders" -> Extract accountId from system message -> call list_orders(accountId="<the-uuid>")
- "my pending orders" -> call list_orders(accountId="<the-uuid>", status="PENDING")
- "order details 68e98226" -> call list_orders(accountId="<the-uuid>") to find matching orders
- "create order for 2x BRK-001" -> call create_b2b_order(accountId="<the-uuid>", items=[...])

Rules:
- NEVER ask the user for their accountId - it is in the system message
- ALWAYS extract the accountId from the system message and pass it to tools
- Present orders in a clear, readable format
- Execute orders without asking for human approval (full autonomy)
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
