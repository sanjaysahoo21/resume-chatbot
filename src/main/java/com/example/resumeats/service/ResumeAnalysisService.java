package com.example.resumeats.service;

import com.example.resumeats.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates the complete ATS analysis pipeline.
 *
 * Pipeline:
 *   1. LLM extracts structured ResumeData
 *   2. LLM extracts structured JobDescriptionData
 *   3. AtsScoringService calculates deterministic scores
 *   4. LLM generates corrections based on scores
 *   5. CourseRecommendationService matches missing skills to courses
 *   6. AnalysisResult is assembled and returned
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeAnalysisService {

    private final LlmService llmService;
    private final AtsScoringService atsScoringService;
    private final CourseRecommendationService courseRecommendationService;

    /**
     * Runs the full analysis asynchronously.
     *
     * @param resumeText      raw extracted text from the resume
     * @param jobDescription  raw job description text
     * @return AnalysisResult wrapped in a CompletableFuture
     */
    @Async
    public CompletableFuture<AnalysisResult> analyzeAsync(String resumeText, String jobDescription) {
        log.info("Starting ATS analysis pipeline");

        try {
            AnalysisResult result = analyze(resumeText, jobDescription);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Analysis pipeline failed: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Runs the full analysis synchronously (used internally and for testing).
     */
    public AnalysisResult analyze(String resumeText, String jobDescription) {
        // Step 1: Extract structured resume data via LLM
        log.debug("Step 1: Extracting resume data");
        ResumeData resumeData = llmService.analyzeResume(resumeText);

        // Step 2: Extract structured JD data via LLM
        log.debug("Step 2: Extracting job description data");
        JobDescriptionData jdData = llmService.analyzeJobDescription(jobDescription);

        // Step 3: Calculate deterministic ATS score (NOT done by LLM)
        log.debug("Step 3: Calculating deterministic ATS score");
        AtsResult atsResult = atsScoringService.calculate(resumeData, jdData);

        // Step 4: Generate corrections via LLM
        log.debug("Step 4: Generating resume corrections");
        CorrectionResult correctionResult = llmService.generateCorrections(resumeData, jdData, atsResult);

        // Step 5: Recommend courses for missing skills
        log.debug("Step 5: Recommending courses for missing skills");
        List<CourseRecommendation> courses = courseRecommendationService.recommend(
                atsResult,
                jdData.getRequiredSkills(),
                jdData.getPreferredSkills()
        );

        log.info("Analysis complete. Overall ATS score: {}/100", atsResult.getOverallScore());

        return AnalysisResult.builder()
                .resumeData(resumeData)
                .jobDescriptionData(jdData)
                .atsResult(atsResult)
                .correctionResult(correctionResult)
                .courseRecommendations(courses)
                .build();
    }
}
