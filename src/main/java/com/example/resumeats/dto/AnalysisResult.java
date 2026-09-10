package com.example.resumeats.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Final complete analysis result that bundles all pipeline outputs together.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    private ResumeData resumeData;
    private JobDescriptionData jobDescriptionData;
    private AtsResult atsResult;
    private CorrectionResult correctionResult;
    private List<CourseRecommendation> courseRecommendations;
}
