package com.example.resumeats.service;

import com.example.resumeats.dto.CorrectionResult;
import com.example.resumeats.dto.JobDescriptionData;
import com.example.resumeats.dto.ResumeData;
import com.example.resumeats.dto.AtsResult;

/**
 * Abstraction for all LLM API interactions.
 * All LLM calls must go through this interface — do not scatter LLM calls in other services.
 */
public interface LlmService {

    /**
     * Extracts structured data from raw resume text.
     *
     * @param resumeText raw text extracted from the resume file
     * @return structured ResumeData DTO
     */
    ResumeData analyzeResume(String resumeText);

    /**
     * Extracts structured requirements from a job description.
     *
     * @param jobDescription raw job description text
     * @return structured JobDescriptionData DTO
     */
    JobDescriptionData analyzeJobDescription(String jobDescription);

    /**
     * Generates specific, actionable resume corrections and an overall assessment.
     *
     * @param resume    structured resume data
     * @param jd        structured job description data
     * @param atsResult deterministic ATS scoring result
     * @return corrections and overall assessment
     */
    CorrectionResult generateCorrections(ResumeData resume, JobDescriptionData jd, AtsResult atsResult);
}
