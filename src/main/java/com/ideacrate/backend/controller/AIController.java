package com.ideacrate.backend.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple mock AI endpoint so you can exercise the AI-powered
 * project summary and tagging flow without calling an external
 * provider.
 *
 * It accepts the same payload that AIClientService sends:
 *  - title
 *  - description
 *  - detailedDescription
 *  - techStack (optional List<String>)
 *
 * and returns JSON in the expected format:
 * {
 *   "summary": {
 *     "problem": "...",
 *     "solution": "...",
 *     "technologies": "...",
 *     "impact": "..."
 *   },
 *   "tags": ["Python", "Machine Learning", ...]
 * }
 */
@RestController
@RequestMapping("/internal/ai")
public class AIController {

    @PostMapping("/analyze")
    public Map<String, Object> analyze(@RequestBody Map<String, Object> payload) {
        String title = asString(payload.get("title"));
        String description = asString(payload.get("description"));
        String detailed = asString(payload.get("detailedDescription"));

        List<String> techStack = extractTechStack(payload.get("techStack"));

        // Build a simple, deterministic "AI" summary based on inputs.
        String problem = !description.isBlank()
                ? description
                : "This project addresses a problem in the domain described by the student.";

        String solution = "This project proposes a solution called '" + title + "' that leverages the described approach to solve the problem.";

        String technologies = techStack.isEmpty()
                ? "Technologies not explicitly specified."
                : String.join(", ", techStack);

        String impact = "The project is expected to create meaningful impact in its target domain by improving existing workflows or enabling new capabilities.";

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("problem", problem);
        summary.put("solution", solution);
        summary.put("technologies", technologies);
        summary.put("impact", impact);

        // Tags: start from tech stack, add a couple of generic/domain tags derived from the title/description.
        Set<String> tags = new LinkedHashSet<>(techStack);

        String lowerTitle = title.toLowerCase(Locale.ROOT);
        String lowerDesc = (description + " " + detailed).toLowerCase(Locale.ROOT);

        if (lowerTitle.contains("ml") || lowerTitle.contains("machine learning") || lowerDesc.contains("model")) {
            tags.add("Machine Learning");
        }
        if (lowerTitle.contains("ai") || lowerDesc.contains("ai")) {
            tags.add("Artificial Intelligence");
        }
        if (lowerDesc.contains("health") || lowerTitle.contains("health")) {
            tags.add("Healthcare");
        }
        if (lowerDesc.contains("web") || lowerTitle.contains("web")) {
            tags.add("Web Application");
        }

        // Ensure tags are clean, unique, and non-empty.
        List<String> tagList = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("tags", tagList);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTechStack(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?>) {
            return ((List<?>) raw).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        // Fallback if techStack comes as a single comma-separated string
        String value = raw.toString();
        if (value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String asString(Object value) {
        if (value == null) return "";
        String s = value.toString();
        return s != null ? s.trim() : "";
    }
}
