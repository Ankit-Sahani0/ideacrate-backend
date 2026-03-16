package com.ideacrate.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Low-level HTTP client for calling an external AI API to analyze projects.
 *
 * This service is intentionally generic: configure the endpoint and API key via
 * application properties:
 *   ai.api.url=https://your-ai-endpoint
 *   ai.api.key=YOUR_API_KEY
 *
 * The endpoint is expected to return JSON in the shape:
 * {
 *   "summary": {
 *     "problem": "...",
 *     "solution": "...",
 *     "technologies": "...",
 *     "impact": "..."
 *   },
 *   "tags": ["Python", "Machine Learning", "AI"]
 * }
 */
@Service
public class AIClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.api.url:}")
    private String aiApiUrl;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    public AIClientService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public Optional<AiResponse> analyzeProject(String title,
                                               String description,
                                               String detailedDescription,
                                               List<String> techStack) {
        // Gemini integration: expects ai.api.url to be the Gemini generateContent endpoint,
        // and ai.api.key to be the Gemini API key.
        if (aiApiUrl == null || aiApiUrl.isBlank() || aiApiKey == null || aiApiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an assistant that summarizes university student software projects.\n");
            prompt.append("Given the project details, return ONLY valid JSON with this exact shape: ");
            prompt.append("{\"summary\":{\"problem\":\"string\",\"solution\":\"string\",\"technologies\":\"string\",\"impact\":\"string\"},\"tags\":[\"string\", ...]}\n\n");

            prompt.append("Title: ").append(Optional.ofNullable(title).orElse(""));
            prompt.append("\nShort description: ").append(Optional.ofNullable(description).orElse(""));
            prompt.append("\nDetailed description: ").append(Optional.ofNullable(detailedDescription).orElse(""));
            if (techStack != null && !techStack.isEmpty()) {
                prompt.append("\nTech stack: ").append(String.join(", ", techStack));
            }
            prompt.append("\n\nRespond with JSON only, no extra text.");

            Map<String, Object> contentPart = Map.of("text", prompt.toString());
            Map<String, Object> content = Map.of("parts", List.of(contentPart));
            Map<String, Object> requestBody = Map.of("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Gemini expects the API key as a query parameter: ?key=API_KEY
            String urlWithKey = aiApiUrl.contains("?")
                    ? aiApiUrl + "&key=" + aiApiKey
                    : aiApiUrl + "?key=" + aiApiKey;

            ResponseEntity<String> response = restTemplate.postForEntity(urlWithKey, requestEntity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return Optional.empty();
            }

            // Gemini returns model output as content.parts[].text. We concatenate all text parts.
            JsonNode firstCandidate = candidates.get(0);
            JsonNode contentNode = firstCandidate.path("content");
            JsonNode parts = contentNode.path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return Optional.empty();
            }

            StringBuilder jsonTextBuilder = new StringBuilder();
            for (JsonNode part : parts) {
                JsonNode textNode = part.get("text");
                if (textNode != null && textNode.isTextual()) {
                    jsonTextBuilder.append(textNode.asText());
                }
            }

            String jsonText = jsonTextBuilder.toString().trim();
            if (jsonText.isEmpty()) {
                return Optional.empty();
            }

            // Now parse the model's JSON output.
            JsonNode jsonRoot = objectMapper.readTree(jsonText);
            JsonNode summaryNode = jsonRoot.path("summary");

            AiSummary summary = new AiSummary(
                    textOrNull(summaryNode, "problem"),
                    textOrNull(summaryNode, "solution"),
                    textOrNull(summaryNode, "technologies"),
                    textOrNull(summaryNode, "impact")
            );

            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = jsonRoot.path("tags");
            if (tagsNode.isArray()) {
                for (JsonNode node : tagsNode) {
                    if (node.isTextual()) {
                        String value = node.asText().trim();
                        if (!value.isEmpty()) {
                            tags.add(value);
                        }
                    }
                }
            }

            return Optional.of(new AiResponse(summary, tags));
        } catch (Exception ex) {
            // On any parsing/network error, fail gracefully and let callers continue without AI.
            return Optional.empty();
        }
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return value != null && !value.isBlank() ? value : null;
    }

    public static class AiSummary {
        private final String problem;
        private final String solution;
        private final String technologies;
        private final String impact;

        public AiSummary(String problem, String solution, String technologies, String impact) {
            this.problem = problem;
            this.solution = solution;
            this.technologies = technologies;
            this.impact = impact;
        }

        public String getProblem() {
            return problem;
        }

        public String getSolution() {
            return solution;
        }

        public String getTechnologies() {
            return technologies;
        }

        public String getImpact() {
            return impact;
        }
    }

    public static class AiResponse {
        private final AiSummary summary;
        private final List<String> tags;

        public AiResponse(AiSummary summary, List<String> tags) {
            this.summary = summary;
            this.tags = tags != null ? Collections.unmodifiableList(new ArrayList<>(tags)) : List.of();
        }

        public AiSummary getSummary() {
            return summary;
        }

        public List<String> getTags() {
            return tags;
        }
    }
}
