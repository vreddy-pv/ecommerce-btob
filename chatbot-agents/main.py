"""
Main FastAPI application for B2B Auto Parts Chatbot.
Orchestrates multi-agent system with specialized agents for orders and catalog.
"""
import os
import asyncio
from contextlib import asynccontextmanager
from typing import Dict, List, Optional

from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials
from pydantic import BaseModel
from dotenv import load_dotenv

from mcp_client import mcp_manager
from orders_agent import create_orders_agent
from catalog_agent import create_catalog_agent
from supervisor import create_supervisor_graph
from auth import get_current_user, get_account_id_from_token

load_dotenv()

# Global agents
orders_agent = None
catalog_agent = None
supervisor_graph = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize MCP connections and agents on startup."""
    global orders_agent, catalog_agent, supervisor_graph

    print("Starting B2B Auto Parts Chatbot...")

    # Connect to MCP servers
    server_configs = {
        "order-service": {
            "url": os.getenv("ORDER_MCP_URL", "http://localhost:8083/sse")
        },
        "catalog-service": {
            "url": os.getenv("CATALOG_MCP_URL", "http://localhost:8082/sse")
        }
    }

    await mcp_manager.connect_to_servers(server_configs)

    # Create specialized agents
    order_tools = mcp_manager.get_tools_for_server("order-service")
    catalog_tools = mcp_manager.get_tools_for_server("catalog-service")

    if order_tools:
        orders_agent = create_orders_agent(order_tools)
        print("Orders Agent initialized with tools:", [t.name for t in order_tools])
    else:
        print("WARNING: No order tools available")

    if catalog_tools:
        catalog_agent = create_catalog_agent(catalog_tools)
        print("Catalog Agent initialized with tools:", [t.name for t in catalog_tools])
    else:
        print("WARNING: No catalog tools available")

    # Create supervisor graph
    if orders_agent and catalog_agent:
        supervisor_graph = create_supervisor_graph(orders_agent, catalog_agent)
        print("Supervisor graph initialized")
    else:
        print("WARNING: Supervisor graph not initialized - some agents missing")

    yield

    # Cleanup
    await mcp_manager.close()
    print("Chatbot shutdown complete")


# Create FastAPI app
app = FastAPI(
    title="B2B Auto Parts Chatbot",
    description="Multi-agent AI chatbot for B2B auto parts e-commerce",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Request/Response models
class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    session_id: Optional[str] = None


class ChatResponse(BaseModel):
    response: str
    agent_used: str
    session_id: Optional[str] = None


# In-memory session storage
sessions: Dict[str, List[ChatMessage]] = {}


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "agents": {
            "orders": orders_agent is not None,
            "catalog": catalog_agent is not None,
            "supervisor": supervisor_graph is not None
        },
        "mcp_servers": list(mcp_manager.tools.keys())
    }


@app.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(get_current_user)
):
    """
    Main chat endpoint. Routes messages to appropriate agent.
    Requires valid JWT token in Authorization header.
    """
    if not supervisor_graph:
        raise HTTPException(
            status_code=503,
            detail="Chatbot agents not initialized. Check MCP server connections."
        )

    # Extract accountId from JWT
    token = credentials.credentials if credentials else None
    account_id = get_account_id_from_token(token) if token else None

    if not account_id:
        raise HTTPException(
            status_code=401,
            detail="Invalid token or missing accountId"
        )

    # Get or create session
    session_id = request.session_id or account_id
    if session_id not in sessions:
        sessions[session_id] = []

    # Add user message to session
    user_message = request.messages[-1] if request.messages else None
    if user_message:
        sessions[session_id].append({"role": "user", "content": user_message.content})

    try:
        # Run supervisor graph
        initial_state = {
            "messages": sessions[session_id],
            "next_agent": "",
            "final_response": ""
        }

        result = await supervisor_graph.ainvoke(initial_state)

        # Extract response - handle LangChain message objects
        final_response = result.get("final_response", "")
        next_agent = result.get("next_agent", "unknown")

        # If no final_response, check messages in result
        if not final_response and "messages" in result:
            agent_messages = result["messages"]
            if agent_messages:
                last_msg = agent_messages[-1]
                # Handle different message types
                if hasattr(last_msg, 'content'):
                    final_response = last_msg.content
                elif isinstance(last_msg, dict):
                    final_response = last_msg.get('content', str(last_msg))
                else:
                    final_response = str(last_msg)

        # If still empty, provide a default response
        if not final_response:
            final_response = "I'm sorry, I couldn't process your request. Please try again."

        # Add response to session
        sessions[session_id].append({"role": "assistant", "content": final_response})

        return ChatResponse(
            response=final_response,
            agent_used=next_agent,
            session_id=session_id
        )

    except Exception as e:
        print(f"Chat error: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/chat/send", response_model=ChatResponse)
async def chat_send(
    request: ChatRequest,
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(get_current_user)
):
    """Alias for /chat endpoint - matches /api/chat/** gateway route."""
    return await chat(request, credentials)


@app.post("/chat/direct")
async def chat_direct(request: ChatRequest):
    """
    Direct chat endpoint - bypasses supervisor, uses specific agent.
    Useful for testing individual agents.
    """
    agent_type = request.session_id or "orders"  # Use session_id to specify agent

    if agent_type == "orders" and orders_agent:
        agent = orders_agent
    elif agent_type == "catalog" and catalog_agent:
        agent = catalog_agent
    else:
        raise HTTPException(status_code=400, detail=f"Agent '{agent_type}' not available")

    user_message = request.messages[-1].content if request.messages else ""

    try:
        result = await agent.ainvoke({"messages": [{"role": "user", "content": user_message}]})

        response = ""
        if "messages" in result and result["messages"]:
            response = result["messages"][-1].content

        return {"response": response, "agent_used": agent_type}

    except Exception as e:
        print(f"Direct chat error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/agents")
async def list_agents():
    """List available agents and their tools."""
    return {
        "agents": {
            "orders": {
                "available": orders_agent is not None,
                "tools": [t.name for t in mcp_manager.get_tools_for_server("order-service")]
            },
            "catalog": {
                "available": catalog_agent is not None,
                "tools": [t.name for t in mcp_manager.get_tools_for_server("catalog-service")]
            }
        }
    }


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8090))
    uvicorn.run(app, host="0.0.0.0", port=port)
