package com.example.resumeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Structured data extracted from a job description by the LLM.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDescriptionData {

    private String jobTitle;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private String experienceRequired;
    private Integer experienceRequiredMonths; // parsed numeric value
    private List<String> educationRequirements;
    private List<String> responsibilities;
    private List<String> keywords;
}
