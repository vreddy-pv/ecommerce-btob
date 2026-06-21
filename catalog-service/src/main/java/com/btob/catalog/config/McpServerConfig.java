package com.btob.catalog.config;

import com.btob.catalog.mcp.CatalogMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server configuration for catalog-service.
 * Registers catalog management tools with the MCP server.
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider catalogToolsCallbacks(CatalogMcpTools catalogMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(catalogMcpTools)
                .build();
    }
}
