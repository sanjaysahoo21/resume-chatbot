package com.example.resumeats.model;

/**
 * Conversation states for the Telegram bot.
 */
public enum ConversationState {
    START,
    WAITING_FOR_RESUME,
    RESUME_RECEIVED,
    WAITING_FOR_JOB_DESCRIPTION,
    ANALYZING,
    RESULT_READY
}
