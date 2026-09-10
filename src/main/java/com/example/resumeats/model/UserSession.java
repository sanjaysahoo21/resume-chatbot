package com.example.resumeats.model;

import com.example.resumeats.dto.AnalysisResult;
import lombok.Data;

/**
 * In-memory session for a single Telegram user.
 * Stores the current conversation state and any data gathered so far.
 * No database is needed for this MVP - state is kept in memory.
 */
@Data
public class UserSession {

    private final long chatId;

    /** Current state in the conversation flow. */
    private ConversationState state = ConversationState.START;

    /** Raw extracted text from the uploaded resume. */
    private String resumeText;

    /** Original job description text from the user. */
    private String jobDescriptionText;

    /** The completed analysis result, available after ANALYZING → RESULT_READY. */
    private AnalysisResult lastAnalysisResult;

    public UserSession(long chatId) {
        this.chatId = chatId;
    }

    /** Reset the session so the user can start a new analysis. */
    public void reset() {
        this.state = ConversationState.WAITING_FOR_RESUME;
        this.resumeText = null;
        this.jobDescriptionText = null;
        this.lastAnalysisResult = null;
    }
}
