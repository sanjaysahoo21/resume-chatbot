package com.example.resumeats.service;

import com.example.resumeats.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the deterministic ATS scoring engine.
 * Verifies weighted calculations and edge cases.
 */
class AtsScoringServiceTest {

    private AtsScoringService service;

    @BeforeEach
    void setUp() {
        service = new AtsScoringService();
    }

    // -----------------------------------------------------------------------
    // Skill Matching Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Full skill match should produce 100% skill score")
    void fullSkillMatch() {
        ResumeData resume = resumeWith(List.of("Java", "Spring Boot", "Docker"));
        JobDescriptionData jd = jdWith(List.of("Java", "Spring Boot", "Docker"), List.of());

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getSkillScore()).isEqualTo(100);
        assertThat(result.getMissingSkills()).isEmpty();
        assertThat(result.getMatchedSkills()).hasSize(3);
    }

    @Test
    @DisplayName("No skill match should produce 0% skill score")
    void noSkillMatch() {
        ResumeData resume = resumeWith(List.of("Python", "Django"));
        JobDescriptionData jd = jdWith(List.of("Java", "Spring Boot", "Kafka"), List.of());

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getSkillScore()).isEqualTo(0);
        assertThat(result.getMissingSkills()).hasSize(3);
        assertThat(result.getMatchedSkills()).isEmpty();
    }

    @Test
    @DisplayName("Partial skill match should produce proportional score")
    void partialSkillMatch() {
        ResumeData resume = resumeWith(List.of("Java", "Spring Boot"));
        JobDescriptionData jd = jdWith(List.of("Java", "Spring Boot", "Kafka", "AWS"), List.of());

        AtsResult result = service.calculate(resume, jd);

        // 2/4 = 50%
        assertThat(result.getSkillScore()).isEqualTo(50);
        assertThat(result.getMissingSkills()).hasSize(2);
        assertThat(result.getMissingSkills()).containsExactlyInAnyOrder("Kafka", "AWS");
    }

    @Test
    @DisplayName("Skill matching should be case-insensitive")
    void caseInsensitiveSkillMatch() {
        ResumeData resume = resumeWith(List.of("JAVA", "spring boot", "Docker"));
        JobDescriptionData jd = jdWith(List.of("java", "Spring Boot", "DOCKER"), List.of());

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getSkillScore()).isEqualTo(100);
    }

    // -----------------------------------------------------------------------
    // Experience Matching Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("No experience requirement should give 100% experience score")
    void noExperienceRequirement() {
        ResumeData resume = resumeWith(List.of("Java"));
        JobDescriptionData jd = jdWith(List.of("Java"), List.of());
        // experienceRequiredMonths = null

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getExperienceScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("Resume meeting experience requirement should score 100%")
    void experienceRequirementMet() {
        ResumeData resume = resumeWithExperience(List.of("Java"), 30); // 30 months
        JobDescriptionData jd = jdWithExperience(List.of("Java"), 24); // 24 months required

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getExperienceScore()).isEqualTo(100);
        assertThat(result.isExperienceMet()).isTrue();
    }

    @Test
    @DisplayName("Under-experienced candidate should have reduced experience score")
    void experienceRequirementNotMet() {
        ResumeData resume = resumeWithExperience(List.of("Java"), 12); // 12 months
        JobDescriptionData jd = jdWithExperience(List.of("Java"), 24); // 24 months required

        AtsResult result = service.calculate(resume, jd);

        // 12/24 = 50% ratio, * 90 = 45
        assertThat(result.getExperienceScore()).isLessThan(80);
        assertThat(result.isExperienceMet()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Education Matching Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("No education requirement should give 100% education score")
    void noEducationRequirement() {
        ResumeData resume = resumeWith(List.of("Java"));
        JobDescriptionData jd = jdWith(List.of("Java"), List.of());

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getEducationScore()).isEqualTo(100);
    }

    // -----------------------------------------------------------------------
    // Keyword Matching Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Keyword matching should be case-insensitive")
    void caseInsensitiveKeywordMatch() {
        ResumeData resume = resumeWith(List.of("Java", "REST API"));
        JobDescriptionData jd = jdWithKeywords(List.of("Java", "REST API"), List.of("java", "rest api"));

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getKeywordScore()).isEqualTo(100);
    }

    // -----------------------------------------------------------------------
    // Overall Score Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Overall score should be within 0-100")
    void overallScoreInRange() {
        ResumeData resume = resumeWith(List.of("Java"));
        JobDescriptionData jd = jdWith(List.of("Java", "Kafka", "AWS"), List.of());

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getOverallScore()).isBetween(0, 100);
        assertThat(result.getSkillScore()).isBetween(0, 100);
        assertThat(result.getKeywordScore()).isBetween(0, 100);
        assertThat(result.getExperienceScore()).isBetween(0, 100);
        assertThat(result.getStructureScore()).isBetween(0, 100);
        assertThat(result.getEducationScore()).isBetween(0, 100);
    }

    @Test
    @DisplayName("Perfect resume should produce high overall score")
    void perfectResumeScore() {
        ResumeData resume = ResumeData.builder()
                .name("John Doe")
                .summary("Java backend developer with 3 years experience")
                .skills(List.of("Java", "Spring Boot", "Docker", "REST API"))
                .experience(List.of(
                        ResumeData.ExperienceEntry.builder()
                                .company("TechCorp")
                                .role("Backend Developer")
                                .durationMonths(36)
                                .description("Built REST APIs with Java and Spring Boot")
                                .build()
                ))
                .education(List.of(
                        ResumeData.EducationEntry.builder()
                                .degree("B.Tech")
                                .field("Computer Science")
                                .institution("University")
                                .build()
                ))
                .projects(List.of("E-commerce backend"))
                .certifications(List.of("AWS Certified Developer"))
                .build();

        JobDescriptionData jd = JobDescriptionData.builder()
                .jobTitle("Java Developer")
                .requiredSkills(List.of("Java", "Spring Boot", "Docker", "REST API"))
                .preferredSkills(List.of())
                .experienceRequiredMonths(24)
                .keywords(List.of("java", "spring boot", "rest api", "docker"))
                .build();

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getOverallScore()).isGreaterThanOrEqualTo(80);
        assertThat(result.getSkillScore()).isEqualTo(100);
    }

    // -----------------------------------------------------------------------
    // File Validation Tests (via ResumeParserService)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("ATS result should always have non-null lists")
    void atsResultListsNonNull() {
        ResumeData resume = resumeWith(null);
        JobDescriptionData jd = jdWith(null, null);

        AtsResult result = service.calculate(resume, jd);

        assertThat(result.getMatchedSkills()).isNotNull();
        assertThat(result.getMissingSkills()).isNotNull();
        assertThat(result.getMatchedKeywords()).isNotNull();
        assertThat(result.getMissingKeywords()).isNotNull();
        assertThat(result.getDetectedSections()).isNotNull();
        assertThat(result.getMissingSections()).isNotNull();
    }

    // -----------------------------------------------------------------------
    // Test helpers
    // -----------------------------------------------------------------------

    private ResumeData resumeWith(List<String> skills) {
        return ResumeData.builder()
                .name("Test User")
                .summary("A developer")
                .skills(skills)
                .experience(List.of())
                .education(List.of())
                .projects(List.of())
                .certifications(List.of())
                .build();
    }

    private ResumeData resumeWithExperience(List<String> skills, int months) {
        return ResumeData.builder()
                .name("Test User")
                .summary("A developer")
                .skills(skills)
                .experience(List.of(
                        ResumeData.ExperienceEntry.builder()
                                .company("Company A")
                                .role("Developer")
                                .durationMonths(months)
                                .build()
                ))
                .education(List.of())
                .projects(List.of())
                .certifications(List.of())
                .build();
    }

    private JobDescriptionData jdWith(List<String> required, List<String> preferred) {
        return JobDescriptionData.builder()
                .jobTitle("Java Developer")
                .requiredSkills(required)
                .preferredSkills(preferred)
                .keywords(List.of())
                .build();
    }

    private JobDescriptionData jdWithExperience(List<String> required, int months) {
        return JobDescriptionData.builder()
                .jobTitle("Java Developer")
                .requiredSkills(required)
                .preferredSkills(List.of())
                .experienceRequiredMonths(months)
                .keywords(List.of())
                .build();
    }

    private JobDescriptionData jdWithKeywords(List<String> required, List<String> keywords) {
        return JobDescriptionData.builder()
                .jobTitle("Java Developer")
                .requiredSkills(required)
                .preferredSkills(List.of())
                .keywords(keywords)
                .build();
    }
}
