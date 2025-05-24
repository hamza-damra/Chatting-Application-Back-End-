package com.chatapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FileStorageConfig {

    private final FileStorageProperties fileStorageProperties;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        initializeUploadDirectories();
    }

    private void initializeUploadDirectories() {
        // List of required subdirectories
        List<String> subdirectories = Arrays.asList(
            "images",
            "documents",
            "audio",
            "video",
            "other"
        );

        Path baseUploadDir = Paths.get(fileStorageProperties.getUploadDir());

        // Create base upload directory if it doesn't exist
        try {
            if (!Files.exists(baseUploadDir)) {
                Files.createDirectories(baseUploadDir);
                log.info("Created base upload directory: {}", baseUploadDir);
            }

            // Create all subdirectories
            for (String subdirectory : subdirectories) {
                Path subdir = baseUploadDir.resolve(subdirectory);
                if (!Files.exists(subdir)) {
                    Files.createDirectories(subdir);
                    log.info("Created subdirectory: {}", subdir);
                }
            }

            // Verify all directories are writable
            if (!Files.isWritable(baseUploadDir)) {
                log.error("Base upload directory is not writable: {}", baseUploadDir);
                throw new IllegalStateException("Base upload directory is not writable: " + baseUploadDir);
            }

            for (String subdirectory : subdirectories) {
                Path subdir = baseUploadDir.resolve(subdirectory);
                if (!Files.isWritable(subdir)) {
                    log.error("Subdirectory is not writable: {}", subdir);
                    throw new IllegalStateException("Subdirectory is not writable: " + subdir);
                }
            }

            log.info("Successfully initialized all upload directories");
        } catch (IOException e) {
            log.error("Failed to create upload directories", e);
            throw new IllegalStateException("Failed to create upload directories", e);
        }
    }
}
