package com.example.resumeats.service;

import com.example.resumeats.config.TelegramConfig;
import com.example.resumeats.dto.AnalysisResult;
import com.example.resumeats.exception.LlmServiceException;
import com.example.resumeats.exception.ResumeParsingException;
import com.example.resumeats.exception.UnsupportedFileTypeException;
import com.example.resumeats.model.ConversationState;
import com.example.resumeats.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Main Telegram bot implementation.
 *
 * Handles the complete conversation flow:
 *   /start → Upload resume → Send JD → Analyze → Show results
 *
 * State is stored in-memory per chatId using ConcurrentHashMap.
 */
@Component
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    private final TelegramConfig telegramConfig;
    private final ResumeParserService resumeParserService;
    private final ResumeAnalysisService analysisService;
    private final ReportGenerator reportGenerator;

    /** In-memory session store: chatId → UserSession */
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public TelegramBotService(TelegramConfig telegramConfig,
                               ResumeParserService resumeParserService,
                               ResumeAnalysisService analysisService,
                               ReportGenerator reportGenerator) {
        super(telegramConfig.getBotToken());
        this.telegramConfig = telegramConfig;
        this.resumeParserService = resumeParserService;
        this.analysisService = analysisService;
        this.reportGenerator = reportGenerator;
    }

    @Override
    public String getBotUsername() {
        return telegramConfig.getBotUsername();
    }

    // -----------------------------------------------------------------------
    // Entry point for all Telegram updates
    // -----------------------------------------------------------------------

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        long chatId = message.getChatId();
        UserSession session = sessions.computeIfAbsent(chatId, UserSession::new);

        if (message.hasText()) {
            handleTextMessage(message, session);
        } else if (message.hasDocument()) {
            handleDocumentMessage(message, session);
        }
    }

    // -----------------------------------------------------------------------
    // Text message handling
    // -----------------------------------------------------------------------

    private void handleTextMessage(Message message, UserSession session) {
        String text = message.getText().trim();
        long chatId = message.getChatId();

        if (text.startsWith("/start")) {
            handleStartCommand(chatId, session);
            return;
        }

        if (text.startsWith("/help")) {
            sendText(chatId, getHelpText());
            return;
        }

        if (text.startsWith("/reset") || text.startsWith("/new")) {
            session.reset();
            sendText(chatId, "🔄 Session reset\\. Please upload your resume to start a new analysis\\.");
            return;
        }

        switch (session.getState()) {
            case WAITING_FOR_JOB_DESCRIPTION -> handleJobDescription(chatId, session, text);
            case ANALYZING -> sendText(chatId, "⏳ Analysis in progress\\. Please wait\\.\\.\\.");
            case RESULT_READY -> sendText(chatId, "✅ Analysis complete\\. Use the buttons above to view details, or /new to start over\\.");
            default -> sendText(chatId, "Please upload your resume in PDF or DOCX format, or type /start to begin\\.");
        }
    }

    // -----------------------------------------------------------------------
    // /start command
    // -----------------------------------------------------------------------

    private void handleStartCommand(long chatId, UserSession session) {
        session.reset();
        session.setState(ConversationState.WAITING_FOR_RESUME);
        sendText(chatId, """
                ━━━━━━━━━━━━━━━━━━━━━━━━
                🤖 *AI Resume ATS Analyzer*
                ━━━━━━━━━━━━━━━━━━━━━━━━
                
                Welcome\\! I help you analyze how well your resume matches a job description\\.
                
                I will provide:
                • 🏆 ATS Compatibility Score
                • ✅ Matched skills
                • ❌ Missing skills
                • ✏️ Resume corrections
                • 🎓 Course recommendations
                
                📎 *Please upload your resume in PDF or DOCX format to begin\\.*
                """);
    }

    // -----------------------------------------------------------------------
    // Document (file) handling
    // -----------------------------------------------------------------------

    private void handleDocumentMessage(Message message, UserSession session) {
        long chatId = message.getChatId();

        if (session.getState() != ConversationState.WAITING_FOR_RESUME
                && session.getState() != ConversationState.RESULT_READY) {
            if (session.getState() == ConversationState.ANALYZING) {
                sendText(chatId, "⏳ Analysis in progress\\. Please wait\\.");
            } else {
                sendText(chatId, "Please type /start first to begin a new analysis\\.");
            }
            return;
        }

        Document doc = message.getDocument();
        String fileName = doc.getFileName();

        log.info("Received document from chatId {}: {}", chatId, fileName);

        sendText(chatId, "📥 Downloading and reading your resume\\.\\.\\.");

        try {
            File tempFile = downloadFile(doc.getFileId(), fileName);
            String resumeText = resumeParserService.parseResume(tempFile, fileName);

            session.setResumeText(resumeText);
            session.setState(ConversationState.RESUME_RECEIVED);

            log.info("Resume parsed for chatId {}. {} characters extracted.", chatId, resumeText.length());

            session.setState(ConversationState.WAITING_FOR_JOB_DESCRIPTION);
            sendText(chatId, """
                    ✅ *Resume received\\!*
                    
                    Now please send the *Job Description* as a text message\\.
                    
                    You can paste the full JD from a job posting\\.
                    """);

        } catch (UnsupportedFileTypeException e) {
            log.warn("Unsupported file from chatId {}: {}", chatId, e.getMessage());
            sendText(chatId, "❌ Unsupported file type\\.\n\nPlease upload a *PDF* or *DOCX* resume\\.");
        } catch (ResumeParsingException e) {
            log.error("Resume parsing failed for chatId {}: {}", chatId, e.getMessage());
            sendText(chatId, "❌ I couldn't read this resume\\.\n\nPlease upload another PDF or DOCX file\\.");
        } catch (Exception e) {
            log.error("Unexpected error handling document for chatId {}: {}", chatId, e.getMessage(), e);
            sendText(chatId, "❌ Something went wrong while processing your file\\. Please try again\\.");
        }
    }

    // -----------------------------------------------------------------------
    // Job Description handling
    // -----------------------------------------------------------------------

    private void handleJobDescription(long chatId, UserSession session, String jdText) {
        if (jdText.isBlank()) {
            sendText(chatId, "❌ Job Description cannot be empty\\. Please paste the job description text\\.");
            return;
        }

        if (jdText.length() < 50) {
            sendText(chatId, "⚠️ The job description seems too short\\. Please paste the full job description\\.");
            return;
        }

        if (jdText.length() > 8000) {
            jdText = jdText.substring(0, 8000);
            sendText(chatId, "ℹ️ Job description was trimmed to 8000 characters for processing\\.");
        }

        session.setJobDescriptionText(jdText);
        session.setState(ConversationState.ANALYZING);

        sendText(chatId, "✅ *Job Description received\\!*\n\n⏳ *Analyzing your resume\\.\\.\\.*\n\nThis may take 30\\-60 seconds\\.");

        log.info("Starting async analysis for chatId {}", chatId);

        String finalJdText = jdText;
        analysisService.analyzeAsync(session.getResumeText(), finalJdText)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Analysis failed for chatId {}: {}", chatId, ex.getMessage(), ex);
                        session.setState(ConversationState.WAITING_FOR_JOB_DESCRIPTION);
                        String errorMsg = "❌ *Resume analysis failed\\.*\n\n";
                        if (ex.getCause() instanceof LlmServiceException) {
                            errorMsg += "The AI service is temporarily unavailable\\. Please try again in a moment\\.";
                        } else {
                            errorMsg += "An error occurred\\. Please try again or type /new to restart\\.";
                        }
                        sendText(chatId, errorMsg);
                    } else {
                        session.setLastAnalysisResult(result);
                        session.setState(ConversationState.RESULT_READY);
                        log.info("Analysis complete for chatId {}. Score: {}/100", chatId, result.getAtsResult().getOverallScore());
                        sendAnalysisResult(chatId, result);
                    }
                });
    }

    // -----------------------------------------------------------------------
    // Send analysis results with buttons
    // -----------------------------------------------------------------------

    private void sendAnalysisResult(long chatId, AnalysisResult result) {
        String report = reportGenerator.generateMainReport(result);
        sendMarkdownWithButtons(chatId, report);
    }

    private void sendMarkdownWithButtons(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("MarkdownV2")
                .replyMarkup(buildResultButtons())
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send result message to chatId {}: {}", chatId, e.getMessage());
            // Fallback: send without markdown
            sendPlainText(chatId, "Analysis complete! Your ATS score: " +
                    "Please check the details using the buttons below.");
        }
    }

    private InlineKeyboardMarkup buildResultButtons() {
        InlineKeyboardButton analyticsBtn = InlineKeyboardButton.builder()
                .text("📊 Detailed Analytics")
                .callbackData("ANALYTICS")
                .build();

        InlineKeyboardButton correctionsBtn = InlineKeyboardButton.builder()
                .text("✏️ Corrections")
                .callbackData("CORRECTIONS")
                .build();

        InlineKeyboardButton coursesBtn = InlineKeyboardButton.builder()
                .text("🎓 Courses")
                .callbackData("COURSES")
                .build();

        InlineKeyboardButton missingBtn = InlineKeyboardButton.builder()
                .text("❌ Missing Skills")
                .callbackData("MISSING_SKILLS")
                .build();

        InlineKeyboardButton newBtn = InlineKeyboardButton.builder()
                .text("🔄 New Analysis")
                .callbackData("NEW_ANALYSIS")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(analyticsBtn, correctionsBtn))
                .keyboardRow(List.of(coursesBtn, missingBtn))
                .keyboardRow(List.of(newBtn))
                .build();
    }

    // -----------------------------------------------------------------------
    // Inline button callbacks
    // -----------------------------------------------------------------------

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();
        UserSession session = sessions.get(chatId);

        if (session == null || session.getLastAnalysisResult() == null) {
            sendText(chatId, "No analysis found\\. Please type /start to begin\\.");
            return;
        }

        AnalysisResult result = session.getLastAnalysisResult();

        switch (data) {
            case "ANALYTICS"     -> sendText(chatId, reportGenerator.generateAnalyticsReport(result));
            case "CORRECTIONS"   -> sendText(chatId, reportGenerator.generateCorrectionsReport(result));
            case "COURSES"       -> sendText(chatId, reportGenerator.generateCoursesReport(result));
            case "MISSING_SKILLS" -> sendText(chatId, reportGenerator.generateMissingSkillsReport(result));
            case "NEW_ANALYSIS"  -> {
                session.reset();
                sendText(chatId, "🔄 Ready for a new analysis\\!\n\nPlease upload your resume in PDF or DOCX format\\.");
            }
            default -> log.warn("Unknown callback data: {}", data);
        }
    }

    // -----------------------------------------------------------------------
    // File download from Telegram
    // -----------------------------------------------------------------------

    private File downloadFile(String fileId, String fileName) throws IOException, TelegramApiException {
        GetFile getFileRequest = new GetFile(fileId);
        org.telegram.telegrambots.meta.api.objects.File telegramFile = execute(getFileRequest);

        String fileUrl = "https://api.telegram.org/file/bot"
                + telegramConfig.getBotToken() + "/"
                + telegramFile.getFilePath();

        String extension = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.'))
                : ".tmp";

        Path tempPath = Files.createTempFile("resume_", extension);
        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempPath.toFile();
    }

    // -----------------------------------------------------------------------
    // Message sending helpers
    // -----------------------------------------------------------------------

    private void sendText(long chatId, String markdownText) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(markdownText)
                .parseMode("MarkdownV2")
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId {}: {}", chatId, e.getMessage());
            sendPlainText(chatId, markdownText.replaceAll("\\\\.", "").replaceAll("[*_`]", ""));
        }
    }

    private void sendPlainText(long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send plain text to chatId {}: {}", chatId, e.getMessage());
        }
    }

    private String getHelpText() {
        return """
                *Available Commands:*
                
                /start \\- Start a new resume analysis
                /new \\- Start over with a new resume
                /reset \\- Reset your current session
                /help \\- Show this help message
                
                *How to use:*
                1\\. Type /start
                2\\. Upload your resume \\(PDF or DOCX\\)
                3\\. Paste the job description
                4\\. Receive your ATS analysis\\!
                """;
    }
}
