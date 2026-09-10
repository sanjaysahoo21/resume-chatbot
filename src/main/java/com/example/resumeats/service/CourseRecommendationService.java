package com.example.resumeats.service;

import com.example.resumeats.dto.AtsResult;
import com.example.resumeats.dto.CourseRecommendation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommends learning courses based on missing skills.
 * Uses a static course catalog loaded from courses.json.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRecommendationService {

    private final ObjectMapper objectMapper;
    private Map<String, CourseRecommendation> courseCatalog = new HashMap<>();

    @PostConstruct
    public void loadCatalog() {
        try {
            ClassPathResource resource = new ClassPathResource("courses.json");
            List<CourseRecommendation> courses = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<>() {}
            );
            for (CourseRecommendation course : courses) {
                if (course.getSkill() != null) {
                    courseCatalog.put(course.getSkill().toLowerCase().trim(), course);
                }
            }
            log.info("Loaded {} courses into catalog", courseCatalog.size());
        } catch (IOException e) {
            log.error("Failed to load course catalog: {}", e.getMessage());
        }
    }

    /**
     * Returns course recommendations for each missing skill found in the ATS result.
     *
     * @param atsResult   the scored ATS result containing missing skills
     * @param requiredSkills the skills that are required (HIGH priority)
     * @param preferredSkills the skills that are preferred (MEDIUM priority)
     * @return list of course recommendations
     */
    public List<CourseRecommendation> recommend(AtsResult atsResult,
                                                 List<String> requiredSkills,
                                                 List<String> preferredSkills) {
        if (atsResult.getMissingSkills() == null || atsResult.getMissingSkills().isEmpty()) {
            return List.of();
        }

        Set<String> requiredSet = requiredSkills == null ? Set.of() :
                requiredSkills.stream().map(String::toLowerCase).collect(Collectors.toSet());

        List<CourseRecommendation> recommendations = new ArrayList<>();

        for (String missingSkill : atsResult.getMissingSkills()) {
            String key = missingSkill.toLowerCase().trim();
            String priority = requiredSet.contains(key) ? "HIGH" : "MEDIUM";

            CourseRecommendation course = courseCatalog.get(key);
            if (course != null) {
                recommendations.add(CourseRecommendation.builder()
                        .skill(missingSkill)
                        .title(course.getTitle())
                        .provider(course.getProvider())
                        .level(course.getLevel())
                        .reason(course.getReason())
                        .url(course.getUrl())
                        .priority(priority)
                        .build());
            } else {
                // No catalog match — recommend topic-level without fabricating a specific course
                recommendations.add(CourseRecommendation.builder()
                        .skill(missingSkill)
                        .title("Learn " + missingSkill)
                        .provider("Search on Udemy, Coursera, or YouTube")
                        .level("Beginner")
                        .reason("This skill is listed as required in the job description and is missing from your resume.")
                        .url(null)
                        .priority(priority)
                        .build());
            }
        }

        // Sort: HIGH first, then MEDIUM, then LOW
        recommendations.sort(Comparator.comparing(c -> {
            return switch (c.getPriority()) {
                case "HIGH"   -> 0;
                case "MEDIUM" -> 1;
                default       -> 2;
            };
        }));

        return recommendations;
    }
}
