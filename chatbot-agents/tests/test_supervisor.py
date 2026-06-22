import pytest
from unittest.mock import patch, MagicMock


class TestRouteMessage:
    @patch("supervisor.create_supervisor")
    def test_routes_to_orders(self, mock_create_supervisor):
        from supervisor import route_message

        mock_llm = MagicMock()
        mock_response = MagicMock()
        mock_response.content = '{"next_agent": "orders"}'
        mock_llm.invoke.return_value = mock_response
        mock_create_supervisor.return_value = mock_llm

        state = {"messages": [{"role": "user", "content": "What is the status of order 123?"}], "next_agent": "", "final_response": ""}
        result = route_message(state)

        assert result["next_agent"] == "orders"

    @patch("supervisor.create_supervisor")
    def test_routes_to_catalog(self, mock_create_supervisor):
        from supervisor import route_message

        mock_llm = MagicMock()
        mock_response = MagicMock()
        mock_response.content = '{"next_agent": "catalog"}'
        mock_llm.invoke.return_value = mock_response
        mock_create_supervisor.return_value = mock_llm

        state = {"messages": [{"role": "user", "content": "Find brake pads"}], "next_agent": "", "final_response": ""}
        result = route_message(state)

        assert result["next_agent"] == "catalog"

    @patch("supervisor.create_supervisor")
    def test_routes_to_direct(self, mock_create_supervisor):
        from supervisor import route_message

        mock_llm = MagicMock()
        mock_response = MagicMock()
        mock_response.content = '{"next_agent": "direct"}'
        mock_llm.invoke.return_value = mock_response
        mock_create_supervisor.return_value = mock_llm

        state = {"messages": [{"role": "user", "content": "Hello"}], "next_agent": "", "final_response": ""}
        result = route_message(state)

        assert result["next_agent"] == "direct"

    @patch("supervisor.create_supervisor")
    def test_handles_json_in_longer_text(self, mock_create_supervisor):
        from supervisor import route_message

        mock_llm = MagicMock()
        mock_response = MagicMock()
        mock_response.content = "Based on your query, I'll route you. {\"next_agent\": \"orders\"}. Have a good day!"
        mock_llm.invoke.return_value = mock_response
        mock_create_supervisor.return_value = mock_llm

        state = {"messages": [{"role": "user", "content": "Check order 456"}], "next_agent": "", "final_response": ""}
        result = route_message(state)

        assert result["next_agent"] == "orders"

    def test_empty_messages_defaults_to_direct(self):
        from supervisor import route_message

        state = {"messages": [], "next_agent": "", "final_response": ""}
        result = route_message(state)

        assert result["next_agent"] == "direct"
        assert "No message provided" in result["final_response"]
