package com.example.resumeats.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Deterministic ATS scoring result calculated by AtsScoringService.
 * The LLM does NOT calculate these scores.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsResult {

    /** Overall weighted ATS score out of 100. */
    private int overallScore;

    /** Skill match score (30% weight). */
    private int skillScore;

    /** Keyword match score (30% weight). */
    private int keywordScore;

    /** Experience match score (15% weight). */
    private int experienceScore;

    /** Resume structure score (15% weight). */
    private int structureScore;

    /** Education match score (10% weight). */
    private int educationScore;

    /** Skills from JD that are present in the resume. */
    private List<String> matchedSkills;

    /** Skills from JD that are missing from the resume. */
    private List<String> missingSkills;

    /** Keywords from JD found in the resume. */
    private List<String> matchedKeywords;

    /** Keywords from JD missing from the resume. */
    private List<String> missingKeywords;

    /** Whether the required experience is met. */
    private boolean experienceMet;

    /** Sections detected in the resume. */
    private List<String> detectedSections;

    /** Sections that are missing but recommended. */
    private List<String> missingSections;
}
