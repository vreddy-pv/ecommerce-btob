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
- "direct": For general greetings, simple questions, or when you can respond directly

Routing rules:
- If user asks about order status, order creation, or order history → route to "orders"
- If user asks about products, search, pricing, or catalog → route to "catalog"
- If user says hello, hi, thanks, or asks a simple general question → route to "direct"
- If unclear, route to "direct" and ask the user to clarify

Respond with ONLY JSON format (no other text):
{"next_agent": "orders" or "catalog" or "direct"}

Examples:
- "What's the status of order 123?" → {"next_agent": "orders"}
- "Find brake pads" → {"next_agent": "catalog"}
- "Create an order for account 456 with 2x BRK-001" → {"next_agent": "orders"}
- "Hello" → {"next_agent": "direct"}
- "Thanks" → {"next_agent": "direct"}
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

    # Parse response - extract JSON from the response
    try:
        content = response.content if hasattr(response, 'content') else str(response)
        print(f"Supervisor routing response: {content}")

        # Find JSON in the response
        if "{" in content and "}" in content:
            json_start = content.index("{")
            json_end = content.index("}") + 1
            json_str = content[json_start:json_end]
            routing = json.loads(json_str)
            next_agent = routing.get("next_agent", "direct")
            print(f"Routed to: {next_agent}")
            return {
                "next_agent": next_agent,
                "final_response": ""
            }
    except Exception as e:
        print(f"Routing parse error: {e}")

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
    from langchain_groq import ChatGroq

    workflow = StateGraph(SupervisorState)

    # Add nodes
    workflow.add_node("router", route_message)
    workflow.add_node("orders", orders_agent)
    workflow.add_node("catalog", catalog_agent)

    # Direct response handler
    def handle_direct(state: SupervisorState) -> Dict[str, str]:
        """Handle direct responses for greetings and simple queries."""
        llm = ChatGroq(
            model="llama-3.3-70b-versatile",
            temperature=0.7,
            max_tokens=256
        )

        messages = state.get("messages", [])
        if not messages:
            return {"final_response": "Hello! How can I help you today?"}

        last_message = messages[-1]
        user_content = last_message.get("content", "") if isinstance(last_message, dict) else str(last_message)

        # Simple greeting response
        response = llm.invoke([
            {"role": "system", "content": "You are a helpful B2B Auto Parts assistant. Respond briefly and friendly."},
            {"role": "user", "content": user_content}
        ])

        final_response = response.content if hasattr(response, 'content') else str(response)
        return {"final_response": final_response}

    workflow.add_node("direct", handle_direct)

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
            "direct": "direct"
        }
    )

    workflow.add_edge("orders", END)
    workflow.add_edge("catalog", END)
    workflow.add_edge("direct", END)

    return workflow.compile()
