package com.baron.geminimcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s";
    private static final String API_KEY = System.getenv("GEMINI_API_KEY");
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public String handleMcpRequest(@RequestBody JsonNode request) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("GEMINI_API_KEY environment variable is not set");
        }

        String query = request.get("query").asText();
        String url = String.format(GEMINI_API_URL, API_KEY);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonRequest = "{\"contents\": [{\"parts\": [{\"text\": \"" + query + "\"}]}]}";
        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);

        String response = restTemplate.postForObject(url, entity, String.class);
        JsonNode jsonResponse = objectMapper.readTree(response);
        return jsonResponse.at("/candidates/0/content/parts/0/text").asText();
    }
}
