package com.example.resumeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Structured data extracted from a resume by the LLM.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeData {

    private String name;
    private String summary;
    private List<String> skills;
    private List<ExperienceEntry> experience;
    private List<EducationEntry> education;
    private List<String> projects;
    private List<String> certifications;

    /** Total experience in months, calculated from experience entries. */
    private Integer totalExperienceMonths;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExperienceEntry {
        private String company;
        private String role;
        private Integer durationMonths;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EducationEntry {
        private String institution;
        private String degree;
        private String field;
        private Integer graduationYear;
    }
}
