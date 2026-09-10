package com.example.resumeats.service.impl;

import com.example.resumeats.config.LlmConfig;
import com.example.resumeats.dto.AtsResult;
import com.example.resumeats.dto.CorrectionResult;
import com.example.resumeats.dto.JobDescriptionData;
import com.example.resumeats.dto.ResumeData;
import com.example.resumeats.exception.LlmServiceException;
import com.example.resumeats.service.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * LLM service implementation using the Groq API (OpenAI-compatible).
 *
 * Groq endpoint: POST https://api.groq.com/openai/v1/chat/completions
 * Auth header:   Authorization: Bearer <api-key>
 * Request body:  OpenAI chat completions format
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmServiceImpl implements LlmService {

    private final LlmConfig llmConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public ResumeData analyzeResume(String resumeText) {
        log.debug("Calling Groq LLM to analyze resume ({} chars)", resumeText.length());
        String prompt = loadPrompt("prompts/resume-analysis.txt")
                .replace("{RESUME_TEXT}", resumeText);
        String json = callLlm(prompt);
        return parseJson(json, ResumeData.class);
    }

    @Override
    public JobDescriptionData analyzeJobDescription(String jobDescription) {
        log.debug("Calling Groq LLM to analyze job description ({} chars)", jobDescription.length());
        String prompt = loadPrompt("prompts/jd-analysis.txt")
                .replace("{JD_TEXT}", jobDescription);
        String json = callLlm(prompt);
        return parseJson(json, JobDescriptionData.class);
    }

    @Override
    public CorrectionResult generateCorrections(ResumeData resume, JobDescriptionData jd, AtsResult ats) {
        log.debug("Calling Groq LLM to generate corrections. Overall ATS score: {}", ats.getOverallScore());
        try {
            String resumeJson = objectMapper.writeValueAsString(resume);
            String jdJson = objectMapper.writeValueAsString(jd);
            String missingSkills = ats.getMissingSkills() != null
                    ? String.join(", ", ats.getMissingSkills()) : "none";

            String prompt = loadPrompt("prompts/corrections.txt")
                    .replace("{RESUME_DATA}", resumeJson)
                    .replace("{JD_DATA}", jdJson)
                    .replace("{OVERALL_SCORE}", String.valueOf(ats.getOverallScore()))
                    .replace("{SKILL_SCORE}", String.valueOf(ats.getSkillScore()))
                    .replace("{KEYWORD_SCORE}", String.valueOf(ats.getKeywordScore()))
                    .replace("{EXPERIENCE_SCORE}", String.valueOf(ats.getExperienceScore()))
                    .replace("{STRUCTURE_SCORE}", String.valueOf(ats.getStructureScore()))
                    .replace("{MISSING_SKILLS}", missingSkills);

            String json = callLlm(prompt);
            return parseJson(json, CorrectionResult.class);

        } catch (Exception e) {
            log.error("Failed to generate corrections: {}", e.getMessage());
            throw new LlmServiceException("Failed to generate resume corrections.", e);
        }
    }

    // -----------------------------------------------------------------------
    // Groq API call — OpenAI-compatible chat completions format
    // -----------------------------------------------------------------------

    /**
     * Calls the Groq chat completions endpoint and returns the raw text content.
     *
     * Request format:
     * {
     *   "model": "llama-3.3-70b-versatile",
     *   "messages": [{"role": "user", "content": "..."}],
     *   "temperature": 0.1,
     *   "response_format": {"type": "json_object"}
     * }
     */
    private String callLlm(String prompt) {
        String url = llmConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> requestBody = Map.of(
                "model", llmConfig.getModel(),
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are a professional resume and job description analyst. Always respond with valid JSON only. Never include markdown, code fences, or explanations outside the JSON."
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new LlmServiceException("Groq API returned non-success status: " + response.getStatusCode());
            }

            // Parse OpenAI-compatible response: choices[0].message.content
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();

            log.debug("Groq LLM response received ({} chars)", content.length());
            return cleanJsonResponse(content);

        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage(), e);
            throw new LlmServiceException("LLM API call failed: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response into {}: {}", clazz.getSimpleName(), e.getMessage());
            log.debug("Problematic JSON: {}", json);
            throw new LlmServiceException("Failed to parse AI response. Please try again.", e);
        }
    }

    /**
     * Removes markdown code fences if the model wraps its JSON in ```json ... ```.
     */
    private String cleanJsonResponse(String text) {
        if (text == null) return "{}";
        text = text.strip();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.strip();
    }

    private String loadPrompt(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new LlmServiceException("Failed to load prompt template: " + resourcePath, e);
        }
    }
}
