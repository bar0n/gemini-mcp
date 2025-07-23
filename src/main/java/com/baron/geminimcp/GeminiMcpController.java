package com.baron.geminimcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/mcp")
public class GeminiMcpController {
    private static final Logger logger = LoggerFactory.getLogger(GeminiMcpController.class);

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s";
    private static final String API_KEY = System.getenv("GEMINI_API_KEY");
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public GeminiResponse handleMcpRequest(@RequestBody String rawRequest) throws Exception {
        logger.info("Received raw MCP request: {}", rawRequest);
        
        if (API_KEY == null || API_KEY.isEmpty()) {
            logger.error("GEMINI_API_KEY environment variable is not set");
            throw new IllegalStateException("GEMINI_API_KEY environment variable is not set");
        }

        String query;
        if (rawRequest.equals("{{input}}")) {
            logger.warn("Received literal template string, using default query");
            query = "Hello, how can I help you?";
        } else {
            try {
                JsonNode request = objectMapper.readTree(rawRequest);
                logger.info("Parsed MCP request: {}", request);
                
                // Try different possible field names
                if (request.has("query")) {
                    query = request.get("query").asText();
                } else if (request.has("message")) {
                    query = request.get("message").asText();
                } else if (request.has("text")) {
                    query = request.get("text").asText();
                } else if (request.has("content")) {
                    query = request.get("content").asText();
                } else {
                    // If it's a string value, use it directly
                    query = request.asText();
                }
            } catch (Exception e) {
                logger.info("Failed to parse as JSON, treating as plain text: {}", e.getMessage());
                query = rawRequest;
            }
        }
        String url = String.format(GEMINI_API_URL, API_KEY);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonRequest = "{\"contents\": [{\"parts\": [{\"text\": \"" + query + "\"}]}]}";
        logger.info("Sending request to Gemini API: URL={}, Body={}", url, jsonRequest);

        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);

        String response = restTemplate.postForObject(url, entity, String.class);
        logger.info("Received response from Gemini API: {}", response);

        JsonNode jsonResponse = objectMapper.readTree(response);
        String result = jsonResponse.at("/candidates/0/content/parts/0/text").asText();
        logger.info("Final response to client: {}", result);
        return new GeminiResponse(result);
    }
}
