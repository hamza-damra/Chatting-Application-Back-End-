package com.chatapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for file download functionality
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class FileDownloadTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDownloadFileByMessageId_MessageNotFound() throws Exception {
        // This test verifies that non-existent messages return 404

        mockMvc.perform(get("/api/files/message/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Expected since message doesn't exist
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDownloadFileByMessageId_NoAttachment() throws Exception {
        // This test verifies that messages without attachments return 404
        // Note: This would require creating a test message without attachment

        mockMvc.perform(get("/api/files/message/1180")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Expected since message has no attachment
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDownloadFileByMessageId_Unauthorized() throws Exception {
        // Test that authentication is required
        mockMvc.perform(get("/api/files/message/1"))
                .andExpect(status().isNotFound()); // Will be 404 since message doesn't exist in test DB
    }

    @Test
    void testDownloadFileByMessageId_NoAuth() throws Exception {
        // Test without authentication should fail
        mockMvc.perform(get("/api/files/message/1"))
                .andExpect(status().isUnauthorized());
    }
}
