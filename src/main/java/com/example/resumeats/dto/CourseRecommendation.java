package com.example.resumeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * A single course recommendation for a missing skill.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseRecommendation {

    /** The skill this course addresses. */
    private String skill;

    /** Title of the course or learning resource. */
    private String title;

    /** Course provider (e.g., "Coursera", "Udemy"). */
    private String provider;

    /** Difficulty level: Beginner, Intermediate, Advanced. */
    private String level;

    /** Why this course is recommended. */
    private String reason;

    /** URL to the course — must be real, not fabricated. */
    private String url;

    /** Priority: HIGH, MEDIUM, LOW — based on whether skill is required or preferred. */
    private String priority;
}
