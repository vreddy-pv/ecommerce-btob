"""
Supervisor Agent - Routes user messages to specialized agents.
Uses LLM to determine intent and route to Orders or Catalog agent.
"""
from langchain_groq import ChatGroq
from langgraph.graph import StateGraph, START, END
from langgraph.prebuilt import create_react_agent
from typing import TypedDict, Annotated, List, Dict, Any, Literal
import operator
import json

from dotenv import load_dotenv
load_dotenv()


# State for the supervisor graph
class SupervisorState(TypedDict):
    messages: Annotated[List[Dict[str, str]], operator.add]
    next_agent: str
    final_response: str


# System prompt for Supervisor
SUPERVISOR_PROMPT = """
You are a B2B Auto Parts Chatbot Supervisor. Your job is to route user messages to the appropriate specialized agent.

Available agents:
- "orders": For order-related queries (check status, create orders, order history)
- "catalog": For product catalog queries (search products, product details, pricing)

Routing rules:
- If user asks about order status, order creation, or order history → route to "orders"
- If user asks about products, search, pricing, or catalog → route to "catalog"
- If user asks a general greeting or question you can answer directly → respond directly
- If unclear, ask the user to clarify their intent

Respond with JSON format:
{"next_agent": "orders" or "catalog" or "direct", "reason": "brief explanation"}

Examples:
- "What's the status of order 123?" → {"next_agent": "orders", "reason": "Order status query"}
- "Find brake pads" → {"next_agent": "catalog", "reason": "Product search query"}
- "Create an order for account 456 with 2x BRK-001" → {"next_agent": "orders", "reason": "Order creation request"}
- "Hello" → {"next_agent": "direct", "reason": "General greeting"}
"""


def create_supervisor() -> Any:
    """Create the supervisor LLM for routing."""
    llm = ChatGroq(
        model="llama-3.3-70b-versatile",
        temperature=0.0,
        max_tokens=256
    )
    return llm


def route_message(state: SupervisorState) -> Dict[str, str]:
    """Route the user message to the appropriate agent."""
    llm = create_supervisor()

    # Get the last user message
    messages = state.get("messages", [])
    if not messages:
        return {"next_agent": "direct", "final_response": "No message provided."}

    last_message = messages[-1]
    user_content = last_message.get("content", "") if isinstance(last_message, dict) else str(last_message)

    # Ask LLM to route
    response = llm.invoke([
        {"role": "system", "content": SUPERVISOR_PROMPT},
        {"role": "user", "content": user_content}
    ])

    # Parse response
    try:
        # Extract JSON from response
        content = response.content
        if "{" in content:
            json_str = content[content.index("{"):content.index("}") + 1]
            routing = json.loads(json_str)
            return {
                "next_agent": routing.get("next_agent", "direct"),
                "final_response": ""
            }
    except Exception as e:
        print(f"Routing error: {e}")

    return {"next_agent": "direct", "final_response": "I couldn't understand your request. Please try again."}


def create_supervisor_graph(orders_agent: Any, catalog_agent: Any) -> Any:
    """
    Create the supervisor graph that routes to specialized agents.

    Args:
        orders_agent: Compiled Orders Agent
        catalog_agent: Compiled Catalog Agent

    Returns:
        Compiled supervisor graph
    """
    workflow = StateGraph(SupervisorState)

    # Add nodes
    workflow.add_node("router", route_message)
    workflow.add_node("orders", orders_agent)
    workflow.add_node("catalog", catalog_agent)

    # Define edges
    workflow.add_edge(START, "router")

    # Conditional routing based on next_agent
    def route_condition(state: SupervisorState) -> str:
        return state.get("next_agent", "direct")

    workflow.add_conditional_edges(
        "router",
        route_condition,
        {
            "orders": "orders",
            "catalog": "catalog",
            "direct": END
        }
    )

    workflow.add_edge("orders", END)
    workflow.add_edge("catalog", END)

    return workflow.compile()
