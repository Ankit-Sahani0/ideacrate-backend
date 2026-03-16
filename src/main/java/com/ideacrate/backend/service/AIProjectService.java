package com.ideacrate.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideacrate.backend.DTO.ProjectRequestDTO;
import com.ideacrate.backend.entity.Project;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-level AI enrichment service used by the project creation flow.
 *
 * Responsibilities:
 * - Call AIClientService with the project text.
 * - Merge AI tags with the manual tech stack (removing duplicates).
 * - Persist the AI summary and tags onto the Project entity.
 *
 * Importantly, all failures are swallowed so project submission never fails
 * because of AI issues.
 */
@Service
public class AIProjectService {

    private final AIClientService aiClientService;
    private final ObjectMapper objectMapper;

    public AIProjectService(AIClientService aiClientService, ObjectMapper objectMapper) {
        this.aiClientService = aiClientService;
        this.objectMapper = objectMapper;
    }

    /**
     * Enriches the given project instance in-place with AI-generated summary and tags.
     * This method MUST NOT throw, so callers can safely ignore failures.
     */
    public void enrichProjectWithAI(Project project, ProjectRequestDTO request) {
        if (project == null || request == null) {
            return;
        }

        String title = nullSafeTrim(request.getTitle());
        String description = nullSafeTrim(request.getDescription());
        String detailedDescription = nullSafeTrim(request.getDetailedDescription());
        List<String> manualTechStack = parseTechStack(request.getTechStack());

        try {
            aiClientService
                    .analyzeProject(title, description, detailedDescription, manualTechStack)
                    .ifPresent(response -> {
                        // Persist structured summary + tags as JSON in ai_summary.
                        try {
                            Map<String, Object> root = new LinkedHashMap<>();
                            Map<String, Object> summaryMap = new LinkedHashMap<>();
                            AIClientService.AiSummary s = response.getSummary();
                            if (s != null) {
                                summaryMap.put("problem", s.getProblem());
                                summaryMap.put("solution", s.getSolution());
                                summaryMap.put("technologies", s.getTechnologies());
                                summaryMap.put("impact", s.getImpact());
                            }
                            root.put("summary", summaryMap);
                            root.put("tags", response.getTags());

                            String json = objectMapper.writeValueAsString(root);
                            project.setAiSummary(json);
                        } catch (Exception ignored) {
                            // Ignore JSON serialization issues; project will simply have no AI summary.
                        }

                        // Merge manual tech stack with AI tags into ai_tags JSON field.
                        try {
                            List<String> merged = new ArrayList<>();
                            if (manualTechStack != null) {
                                merged.addAll(manualTechStack);
                            }
                            if (response.getTags() != null) {
                                merged.addAll(response.getTags());
                            }

                            List<String> distinct = merged.stream()
                                    .filter(Objects::nonNull)
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .distinct()
                                    .collect(Collectors.toList());

                            if (!distinct.isEmpty()) {
                                // Project.aiTags is now a JSON-mapped List<String>.
                                project.setAiTags(distinct);
                            }
                        } catch (Exception ignored) {
                            // Ignore; absence of AI tags must not break submission.
                        }
                    });
        } catch (Exception ignored) {
            // Any unexpected error must not affect the project creation flow.
        }
    }

    private List<String> parseTechStack(String techStackRaw) {
        if (techStackRaw == null || techStackRaw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(techStackRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String nullSafeTrim(String value) {
        return value == null ? null : value.trim();
    }
}
