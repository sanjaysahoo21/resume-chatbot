package com.example.resumeats.service;

import com.example.resumeats.dto.AtsResult;
import com.example.resumeats.dto.JobDescriptionData;
import com.example.resumeats.dto.ResumeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic ATS scoring engine.
 *
 * Weights:
 *   Skill Match       30%
 *   Keyword Match     30%
 *   Experience Match  15%
 *   Resume Structure  15%
 *   Education Match   10%
 *
 * The LLM does NOT calculate these scores.
 * Every score is explainable and reproducible.
 */
@Service
@Slf4j
public class AtsScoringService {

    private static final List<String> COMMON_SECTIONS = List.of(
            "summary", "objective", "skills", "experience", "work experience",
            "education", "projects", "certifications", "achievements"
    );

    /**
     * Calculates a full ATS result from structured resume and JD data.
     */
    public AtsResult calculate(ResumeData resume, JobDescriptionData jd) {
        log.debug("Calculating ATS score for resume vs JD: {}", jd.getJobTitle());

        // --- Individual component scores ---
        SkillMatchResult skillMatch = calculateSkillScore(resume, jd);
        KeywordMatchResult keywordMatch = calculateKeywordScore(resume, jd);
        int experienceScore = calculateExperienceScore(resume, jd);
        StructureResult structureResult = calculateStructureScore(resume);
        int educationScore = calculateEducationScore(resume, jd);

        // --- Weighted overall score ---
        int overallScore = (int) Math.round(
                (skillMatch.score * 0.30) +
                (keywordMatch.score * 0.30) +
                (experienceScore   * 0.15) +
                (structureResult.score * 0.15) +
                (educationScore    * 0.10)
        );

        log.debug("ATS Score breakdown — Overall: {}, Skill: {}, Keyword: {}, Experience: {}, Structure: {}, Education: {}",
                overallScore, skillMatch.score, keywordMatch.score, experienceScore, structureResult.score, educationScore);

        return AtsResult.builder()
                .overallScore(clamp(overallScore))
                .skillScore(clamp(skillMatch.score))
                .keywordScore(clamp(keywordMatch.score))
                .experienceScore(clamp(experienceScore))
                .structureScore(clamp(structureResult.score))
                .educationScore(clamp(educationScore))
                .matchedSkills(skillMatch.matched)
                .missingSkills(skillMatch.missing)
                .matchedKeywords(keywordMatch.matched)
                .missingKeywords(keywordMatch.missing)
                .experienceMet(experienceScore >= 80)
                .detectedSections(structureResult.detected)
                .missingSections(structureResult.missing)
                .build();
    }

    // -----------------------------------------------------------------------
    // Skill Matching — 30%
    // -----------------------------------------------------------------------

    private SkillMatchResult calculateSkillScore(ResumeData resume, JobDescriptionData jd) {
        List<String> required = normalize(jd.getRequiredSkills());
        List<String> resumeSkills = normalize(resume.getSkills());

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String skill : required) {
            if (resumeSkills.contains(skill) || resumeContains(resume, skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        int score = required.isEmpty() ? 100 : (int) Math.round((double) matched.size() / required.size() * 100);

        // Also check preferred skills but they don't add to score directly
        List<String> preferred = normalize(jd.getPreferredSkills());
        for (String skill : preferred) {
            if (!matched.contains(skill) && resumeSkills.contains(skill)) {
                matched.add(skill);
            }
        }

        return new SkillMatchResult(score, toOriginalCase(matched, jd.getRequiredSkills()), toOriginalCase(missing, jd.getRequiredSkills()));
    }

    // -----------------------------------------------------------------------
    // Keyword Matching — 30%
    // -----------------------------------------------------------------------

    private KeywordMatchResult calculateKeywordScore(ResumeData resume, JobDescriptionData jd) {
        List<String> keywords = normalize(jd.getKeywords());
        if (keywords.isEmpty()) {
            // Fall back to required skills as keywords
            keywords = normalize(jd.getRequiredSkills());
        }

        String resumeText = buildResumeText(resume).toLowerCase();

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String keyword : keywords) {
            if (resumeText.contains(keyword)) {
                matched.add(keyword);
            } else {
                missing.add(keyword);
            }
        }

        int score = keywords.isEmpty() ? 100 : (int) Math.round((double) matched.size() / keywords.size() * 100);
        return new KeywordMatchResult(score, matched, missing);
    }

    // -----------------------------------------------------------------------
    // Experience Matching — 15%
    // -----------------------------------------------------------------------

    private int calculateExperienceScore(ResumeData resume, JobDescriptionData jd) {
        Integer requiredMonths = jd.getExperienceRequiredMonths();
        if (requiredMonths == null || requiredMonths == 0) {
            // No experience requirement specified — no penalty
            return 100;
        }

        int resumeMonths = calculateTotalExperienceMonths(resume);

        if (resumeMonths >= requiredMonths) {
            return 100;
        } else if (resumeMonths == 0) {
            return 0;
        } else {
            // Proportional score with grace — slight under-experience still scores reasonably
            double ratio = (double) resumeMonths / requiredMonths;
            return (int) Math.round(ratio * 90); // max 90 if under-experienced
        }
    }

    private int calculateTotalExperienceMonths(ResumeData resume) {
        if (resume.getExperience() == null || resume.getExperience().isEmpty()) {
            return 0;
        }
        return resume.getExperience().stream()
                .mapToInt(e -> e.getDurationMonths() != null ? e.getDurationMonths() : 0)
                .sum();
    }

    // -----------------------------------------------------------------------
    // Resume Structure — 15%
    // -----------------------------------------------------------------------

    private StructureResult calculateStructureScore(ResumeData resume) {
        String resumeText = buildResumeText(resume).toLowerCase();

        List<String> detected = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        // Check for key sections
        checkSection(resumeText, resume.getSummary(), "Summary/Objective", detected, missing);
        checkSection(resumeText, listToString(resume.getSkills()), "Skills", detected, missing);
        checkSection(resumeText, experienceToString(resume), "Experience", detected, missing);
        checkSection(resumeText, listToString(resume.getProjects()), "Projects", detected, missing);
        checkSection(resumeText, educationToString(resume), "Education", detected, missing);
        checkSection(resumeText, listToString(resume.getCertifications()), "Certifications", detected, missing);

        int score = (int) Math.round((double) detected.size() / (detected.size() + missing.size()) * 100);
        return new StructureResult(score, detected, missing);
    }

    private void checkSection(String resumeText, String content, String sectionName,
                               List<String> detected, List<String> missing) {
        boolean present = content != null && !content.isBlank();
        if (present) {
            detected.add(sectionName);
        } else {
            missing.add(sectionName);
        }
    }

    // -----------------------------------------------------------------------
    // Education Matching — 10%
    // -----------------------------------------------------------------------

    private int calculateEducationScore(ResumeData resume, JobDescriptionData jd) {
        List<String> requirements = jd.getEducationRequirements();
        if (requirements == null || requirements.isEmpty()) {
            // No education requirement — no penalty
            return 100;
        }

        if (resume.getEducation() == null || resume.getEducation().isEmpty()) {
            return 30; // Resume has no education listed
        }

        // Basic check: does the resume contain any of the education keywords?
        String resumeEduText = educationToString(resume).toLowerCase();
        long matched = requirements.stream()
                .map(String::toLowerCase)
                .filter(req -> {
                    String[] words = req.split("\\s+");
                    for (String word : words) {
                        if (word.length() > 3 && resumeEduText.contains(word)) return true;
                    }
                    return false;
                })
                .count();

        return requirements.isEmpty() ? 100 : (int) Math.round((double) matched / requirements.size() * 100);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<String> normalize(List<String> items) {
        if (items == null) return List.of();
        return items.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private List<String> toOriginalCase(List<String> normalized, List<String> originals) {
        if (originals == null) return normalized;
        Set<String> normalizedSet = normalized.stream().collect(Collectors.toSet());
        return originals.stream()
                .filter(o -> normalizedSet.contains(o.toLowerCase().trim()))
                .collect(Collectors.toList());
    }

    private boolean resumeContains(ResumeData resume, String skillNormalized) {
        String resumeText = buildResumeText(resume).toLowerCase();
        return resumeText.contains(skillNormalized);
    }

    private String buildResumeText(ResumeData resume) {
        StringBuilder sb = new StringBuilder();
        if (resume.getName() != null) sb.append(resume.getName()).append(" ");
        if (resume.getSummary() != null) sb.append(resume.getSummary()).append(" ");
        if (resume.getSkills() != null) sb.append(String.join(" ", resume.getSkills())).append(" ");
        if (resume.getProjects() != null) sb.append(String.join(" ", resume.getProjects())).append(" ");
        if (resume.getCertifications() != null) sb.append(String.join(" ", resume.getCertifications())).append(" ");
        if (resume.getExperience() != null) {
            for (ResumeData.ExperienceEntry e : resume.getExperience()) {
                if (e.getRole() != null) sb.append(e.getRole()).append(" ");
                if (e.getCompany() != null) sb.append(e.getCompany()).append(" ");
                if (e.getDescription() != null) sb.append(e.getDescription()).append(" ");
            }
        }
        if (resume.getEducation() != null) {
            for (ResumeData.EducationEntry e : resume.getEducation()) {
                if (e.getDegree() != null) sb.append(e.getDegree()).append(" ");
                if (e.getField() != null) sb.append(e.getField()).append(" ");
            }
        }
        return sb.toString();
    }

    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(", ", list);
    }

    private String experienceToString(ResumeData resume) {
        if (resume.getExperience() == null || resume.getExperience().isEmpty()) return "";
        return resume.getExperience().stream()
                .map(e -> e.getRole() + " at " + e.getCompany())
                .collect(Collectors.joining("; "));
    }

    private String educationToString(ResumeData resume) {
        if (resume.getEducation() == null || resume.getEducation().isEmpty()) return "";
        return resume.getEducation().stream()
                .map(e -> e.getDegree() + " " + e.getField() + " " + e.getInstitution())
                .collect(Collectors.joining("; "));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    // -----------------------------------------------------------------------
    // Inner result classes
    // -----------------------------------------------------------------------

    private record SkillMatchResult(int score, List<String> matched, List<String> missing) {}
    private record KeywordMatchResult(int score, List<String> matched, List<String> missing) {}
    private record StructureResult(int score, List<String> detected, List<String> missing) {}
}
