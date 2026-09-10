package com.example.resumeats.service;

import com.example.resumeats.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Formats AnalysisResult into clean, emoji-rich Telegram messages.
 * Each method produces a self-contained message string.
 */
@Component
public class ReportGenerator {

    private static final String LINE = "━━━━━━━━━━━━━━━━━━━━━━━━";

    /**
     * Generates the main summary report sent after analysis.
     */
    public String generateMainReport(AnalysisResult result) {
        AtsResult ats = result.getAtsResult();
        JobDescriptionData jd = result.getJobDescriptionData();
        CorrectionResult cr = result.getCorrectionResult();

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(LINE).append("\n");
        sb.append("🎯 *RESUME ATS ANALYSIS*\n");
        if (jd.getJobTitle() != null && !jd.getJobTitle().isBlank()) {
            sb.append("📋 Role: ").append(escapeMarkdown(jd.getJobTitle())).append("\n");
        }
        sb.append(LINE).append("\n\n");

        // ATS Score
        sb.append("🏆 *ATS Score: ").append(ats.getOverallScore()).append("/100*");
        sb.append(" ").append(getScoreEmoji(ats.getOverallScore())).append("\n\n");

        // Score Breakdown
        sb.append("📊 *Score Breakdown*\n");
        sb.append(formatScoreBar("Skills Match   ", ats.getSkillScore())).append("\n");
        sb.append(formatScoreBar("Keywords Match ", ats.getKeywordScore())).append("\n");
        sb.append(formatScoreBar("Experience     ", ats.getExperienceScore())).append("\n");
        sb.append(formatScoreBar("Structure      ", ats.getStructureScore())).append("\n");
        sb.append(formatScoreBar("Education      ", ats.getEducationScore())).append("\n");

        // Matched Skills
        if (ats.getMatchedSkills() != null && !ats.getMatchedSkills().isEmpty()) {
            sb.append("\n").append(LINE).append("\n");
            sb.append("✅ *MATCHED SKILLS*\n");
            sb.append(LINE).append("\n");
            for (String skill : ats.getMatchedSkills()) {
                sb.append("• ").append(escapeMarkdown(skill)).append("\n");
            }
        }

        // Missing Skills
        if (ats.getMissingSkills() != null && !ats.getMissingSkills().isEmpty()) {
            sb.append("\n").append(LINE).append("\n");
            sb.append("❌ *MISSING SKILLS*\n");
            sb.append(LINE).append("\n");
            for (String skill : ats.getMissingSkills()) {
                sb.append("🔴 ").append(escapeMarkdown(skill)).append("\n");
            }
        }

        // Key Improvements (top 4)
        if (cr != null && cr.getCorrections() != null && !cr.getCorrections().isEmpty()) {
            sb.append("\n").append(LINE).append("\n");
            sb.append("⚠️ *KEY IMPROVEMENTS*\n");
            sb.append(LINE).append("\n");
            List<Correction> top = cr.getCorrections().subList(0, Math.min(4, cr.getCorrections().size()));
            for (int i = 0; i < top.size(); i++) {
                sb.append(i + 1).append("\\. ").append(escapeMarkdown(top.get(i).getReason())).append("\n");
            }
        }

        // Course Recommendations
        List<CourseRecommendation> courses = result.getCourseRecommendations();
        if (courses != null && !courses.isEmpty()) {
            sb.append("\n").append(LINE).append("\n");
            sb.append("🎓 *RECOMMENDED LEARNING*\n");
            sb.append(LINE).append("\n");
            List<CourseRecommendation> top = courses.subList(0, Math.min(3, courses.size()));
            for (int i = 0; i < top.size(); i++) {
                sb.append(i + 1).append("\\. ").append(escapeMarkdown(top.get(i).getTitle()))
                  .append(" \\[").append(top.get(i).getPriority()).append("\\]\n");
            }
        }

        // Overall Assessment
        if (cr != null && cr.getOverallAssessment() != null && !cr.getOverallAssessment().isBlank()) {
            sb.append("\n").append(LINE).append("\n");
            sb.append("📈 *OVERALL ASSESSMENT*\n");
            sb.append(LINE).append("\n");
            sb.append(escapeMarkdown(cr.getOverallAssessment())).append("\n");
        }

        sb.append("\n").append(LINE);
        return sb.toString();
    }

    /**
     * Generates the detailed corrections message (triggered by button).
     */
    public String generateCorrectionsReport(AnalysisResult result) {
        CorrectionResult cr = result.getCorrectionResult();
        if (cr == null || cr.getCorrections() == null || cr.getCorrections().isEmpty()) {
            return "✅ No specific corrections identified\\. Your resume looks well\\-structured for this role\\.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append("\n");
        sb.append("✏️ *RESUME CORRECTIONS*\n");
        sb.append(LINE).append("\n\n");

        for (int i = 0; i < cr.getCorrections().size(); i++) {
            Correction c = cr.getCorrections().get(i);
            sb.append("*").append(i + 1).append("\\. ").append(escapeMarkdown(c.getSection())).append("*\n");

            if (c.getCurrent() != null && !c.getCurrent().isBlank()) {
                sb.append("📌 *Current:* ").append(escapeMarkdown(c.getCurrent())).append("\n");
            }
            if (c.getSuggested() != null && !c.getSuggested().isBlank()) {
                sb.append("💡 *Suggested:* ").append(escapeMarkdown(c.getSuggested())).append("\n");
            }
            if (c.getReason() != null && !c.getReason().isBlank()) {
                sb.append("📝 *Reason:* ").append(escapeMarkdown(c.getReason())).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Generates the detailed course recommendations message (triggered by button).
     */
    public String generateCoursesReport(AnalysisResult result) {
        List<CourseRecommendation> courses = result.getCourseRecommendations();
        if (courses == null || courses.isEmpty()) {
            return "✅ No missing skills detected\\. Great match for the required skills\\!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append("\n");
        sb.append("🎓 *COURSE RECOMMENDATIONS*\n");
        sb.append(LINE).append("\n\n");

        for (int i = 0; i < courses.size(); i++) {
            CourseRecommendation c = courses.get(i);
            sb.append("*").append(i + 1).append("\\. ").append(escapeMarkdown(c.getTitle())).append("*\n");
            sb.append("🎯 Skill: ").append(escapeMarkdown(c.getSkill())).append("\n");
            sb.append("🏢 Provider: ").append(escapeMarkdown(c.getProvider())).append("\n");
            sb.append("📊 Level: ").append(escapeMarkdown(c.getLevel())).append("\n");
            sb.append("🔥 Priority: ").append(escapeMarkdown(c.getPriority())).append("\n");
            if (c.getUrl() != null && !c.getUrl().isBlank()) {
                sb.append("🔗 [Course Link](").append(c.getUrl()).append(")\n");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Generates the missing skills detail message (triggered by button).
     */
    public String generateMissingSkillsReport(AnalysisResult result) {
        AtsResult ats = result.getAtsResult();
        JobDescriptionData jd = result.getJobDescriptionData();

        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append("\n");
        sb.append("❌ *SKILL GAP ANALYSIS*\n");
        sb.append(LINE).append("\n\n");

        if (ats.getMissingSkills() == null || ats.getMissingSkills().isEmpty()) {
            sb.append("✅ You have all the required skills for this role\\!\n");
        } else {
            sb.append("*Missing Required Skills:*\n");
            for (String skill : ats.getMissingSkills()) {
                sb.append("🔴 ").append(escapeMarkdown(skill)).append("\n");
            }
        }

        if (ats.getMissingKeywords() != null && !ats.getMissingKeywords().isEmpty()) {
            sb.append("\n*Missing Keywords:*\n");
            for (String kw : ats.getMissingKeywords()) {
                sb.append("⚠️ ").append(escapeMarkdown(kw)).append("\n");
            }
        }

        if (ats.getMissingSections() != null && !ats.getMissingSections().isEmpty()) {
            sb.append("\n*Recommended Missing Sections:*\n");
            for (String section : ats.getMissingSections()) {
                sb.append("📝 ").append(escapeMarkdown(section)).append("\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * Generates the detailed analytics message (triggered by button).
     */
    public String generateAnalyticsReport(AnalysisResult result) {
        AtsResult ats = result.getAtsResult();
        ResumeData resume = result.getResumeData();

        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append("\n");
        sb.append("📊 *DETAILED ANALYTICS*\n");
        sb.append(LINE).append("\n\n");

        sb.append("*Score Breakdown:*\n");
        sb.append("• Overall Score: *").append(ats.getOverallScore()).append("/100*\n");
        sb.append("• Skills Match: ").append(ats.getSkillScore()).append("%\n");
        sb.append("• Keywords Match: ").append(ats.getKeywordScore()).append("%\n");
        sb.append("• Experience Match: ").append(ats.getExperienceScore()).append("%\n");
        sb.append("• Resume Structure: ").append(ats.getStructureScore()).append("%\n");
        sb.append("• Education Match: ").append(ats.getEducationScore()).append("%\n");

        sb.append("\n*Skill Summary:*\n");
        int matched = ats.getMatchedSkills() != null ? ats.getMatchedSkills().size() : 0;
        int missing = ats.getMissingSkills() != null ? ats.getMissingSkills().size() : 0;
        sb.append("✅ Matched: ").append(matched).append(" skills\n");
        sb.append("❌ Missing: ").append(missing).append(" skills\n");

        sb.append("\n*Resume Sections Detected:*\n");
        if (ats.getDetectedSections() != null) {
            for (String s : ats.getDetectedSections()) {
                sb.append("✅ ").append(escapeMarkdown(s)).append("\n");
            }
        }
        if (ats.getMissingSections() != null && !ats.getMissingSections().isEmpty()) {
            for (String s : ats.getMissingSections()) {
                sb.append("⚠️ ").append(escapeMarkdown(s)).append(" \\(not detected\\)\n");
            }
        }

        sb.append("\n*Experience:*\n");
        boolean expMet = ats.isExperienceMet();
        sb.append(expMet ? "✅ Experience requirement met\n" : "⚠️ Experience requirement not fully met\n");

        return sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String formatScoreBar(String label, int score) {
        int filled = score / 10;
        String bar = "█".repeat(filled) + "░".repeat(10 - filled);
        return String.format("`%s %s %3d%%`", label, bar, score);
    }

    private String getScoreEmoji(int score) {
        if (score >= 80) return "🟢";
        if (score >= 60) return "🟡";
        return "🔴";
    }

    /**
     * Escapes characters that have special meaning in Telegram MarkdownV2.
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}
