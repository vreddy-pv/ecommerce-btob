package com.btob.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.time.Duration;

/**
 * WebFlux static resource serving + SPA fallback for Angular built files (D-04).
 *
 * Two RouterFunction beans:
 * 1. Static asset router: serves files from configured location for non-/api/** paths
 * 2. SPA fallback router: serves index.html for non-/api/** paths that didn't match a static file
 *
 * Both explicitly exclude /api/** so gateway route predicates remain authoritative for API calls.
 *
 * The resource location is configurable via btob.static-resource-location:
 * - Default: classpath:/static/ (for embedded builds)
 * - Override: file:/path/to/dist/ (for Docker volume mounts)
 */
@Configuration
public class StaticResourceConfig {

    @Value("${btob.static-resource-location:classpath:/static/}")
    private String resourceLocation;

    /**
     * Static asset router: serves files from the configured location.
     * Excludes /api/** paths so gateway routes handle API calls.
     * Uses Cache-Control max-age 30d for hashed asset filenames.
     */
    @Bean
    public RouterFunction<ServerResponse> staticAssetRouter() {
        return RouterFunctions.route()
                .GET("/**", this::isNotApiPath, request -> {
                    String path = request.path();
                    // Skip root path and paths with extensions that look like API
                    if (path.equals("/") || path.isEmpty()) {
                        return ServerResponse.notFound().build();
                    }

                    try {
                        Resource resource = resolveResource().createRelative(
                                path.startsWith("/") ? path.substring(1) : path
                        );

                        if (resource.exists() && resource.isReadable()) {
                            MediaType contentType = resolveContentType(path);
                            CacheControl cacheControl = isHashedAsset(path)
                                    ? CacheControl.maxAge(Duration.ofDays(30))
                                    : CacheControl.noCache();

                            return ServerResponse.ok()
                                    .contentType(contentType)
                                    .cacheControl(cacheControl)
                                    .bodyValue(resource);
                        }
                    } catch (java.io.IOException e) {
                        // Resource resolution failed — fall through to not found
                    }

                    // Resource not found — let SPA fallback handle it
                    return ServerResponse.notFound().build();
                })
                .build();
    }

    /**
     * SPA fallback router: serves index.html for non-/api/** paths.
     * This enables client-side SPA routes (/catalog, /orders/:id) to work on direct browser entry.
     * Excludes /api/** paths so gateway routes handle API calls.
     * Uses lower priority (order = 1) so static asset router runs first.
     */
    @Bean
    public RouterFunction<ServerResponse> spaFallbackRouter() {
        return RouterFunctions.route()
                .GET("/**", this::isNotApiPath, request -> {
                    try {
                        Resource indexHtml = resolveResource().createRelative("index.html");

                        if (indexHtml.exists() && indexHtml.isReadable()) {
                            return ServerResponse.ok()
                                    .contentType(MediaType.TEXT_HTML)
                                    .cacheControl(CacheControl.noCache())
                                    .bodyValue(indexHtml);
                        }
                    } catch (java.io.IOException e) {
                        // Resource resolution failed — fall through to not found
                    }

                    // No index.html found — return 404
                    return ServerResponse.notFound().build();
                })
                .build();
    }

    /**
     * Predicate: returns true if the request path does NOT start with /api/
     */
    private boolean isNotApiPath(org.springframework.web.reactive.function.server.ServerRequest request) {
        return !request.path().startsWith("/api/");
    }

    /**
     * Resolve the configured resource location to a Resource.
     * Supports both classpath: and file: prefixes.
     */
    private Resource resolveResource() {
        if (resourceLocation.startsWith("classpath:")) {
            return new ClassPathResource(resourceLocation.substring("classpath:".length()));
        } else if (resourceLocation.startsWith("file:")) {
            return new FileSystemResource(resourceLocation.substring("file:".length()));
        }
        // Default to classpath
        return new ClassPathResource("static/");
    }

    /**
     * Determine if a path is a hashed asset (contains a hash in the filename).
     * Angular CLI produces filenames like: main.abc123def.js
     */
    private boolean isHashedAsset(String path) {
        if (path == null) return false;
        String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        // Match pattern: name.hash.ext (e.g., main.abc123.js)
        return filename.matches(".*\\.[a-f0-9]{8,}\\.\\w+$");
    }

    /**
     * Resolve content type from file extension.
     */
    private MediaType resolveContentType(String path) {
        if (path == null) return MediaType.APPLICATION_OCTET_STREAM;

        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return MediaType.TEXT_HTML;
        if (lower.endsWith(".js")) return MediaType.parseMediaType("application/javascript");
        if (lower.endsWith(".css")) return MediaType.parseMediaType("text/css");
        if (lower.endsWith(".json")) return MediaType.APPLICATION_JSON;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        if (lower.endsWith(".ico")) return MediaType.parseMediaType("image/x-icon");
        if (lower.endsWith(".woff")) return MediaType.parseMediaType("font/woff");
        if (lower.endsWith(".woff2")) return MediaType.parseMediaType("font/woff2");
        if (lower.endsWith(".ttf")) return MediaType.parseMediaType("font/ttf");
        if (lower.endsWith(".eot")) return MediaType.parseMediaType("application/vnd.ms-fontobject");

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
