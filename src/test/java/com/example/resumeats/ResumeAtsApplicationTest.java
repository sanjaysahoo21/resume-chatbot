package com.example.resumeats;

import com.example.resumeats.service.TelegramBotService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring context load test.
 * Mocks TelegramBotService to avoid real network connections during testing.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "telegram.bot-token=test-token",
        "telegram.bot-username=TestBot",
        "llm.api-key=test-key",
        "llm.base-url=https://example.com",
        "llm.model=test-model"
})
class ResumeAtsApplicationTest {

    @MockBean
    TelegramBotService telegramBotService;

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts without errors.
    }
}
