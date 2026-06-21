package com.btob.order.config;

import com.btob.order.mcp.OrderMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server configuration for order-service.
 * Registers order management tools with the MCP server.
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider orderToolsCallbacks(OrderMcpTools orderMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(orderMcpTools)
                .build();
    }
}
