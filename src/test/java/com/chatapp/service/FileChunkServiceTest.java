package com.chatapp.service;

import com.chatapp.config.FileStorageProperties;
import com.chatapp.model.FileMetadata;
import com.chatapp.model.User;
import com.chatapp.repository.FileMetadataRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.websocket.model.FileChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FileChunkServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileStorageProperties fileStorageProperties;

    @Mock
    private FileMetadataService fileMetadataService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    private FileChunkService fileChunkService;
    private Map<String, Path> contentTypePaths = new HashMap<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create subdirectories
        try {
            Path imagesDir = Files.createDirectory(tempDir.resolve("images"));
            Path documentsDir = Files.createDirectory(tempDir.resolve("documents"));
            Path audioDir = Files.createDirectory(tempDir.resolve("audio"));
            Path videoDir = Files.createDirectory(tempDir.resolve("video"));
            Path otherDir = Files.createDirectory(tempDir.resolve("other"));
            Files.createDirectory(tempDir.resolve("temp"));

            contentTypePaths.put("image/jpeg", imagesDir);
            contentTypePaths.put("image/png", imagesDir);
            contentTypePaths.put("text/plain", documentsDir);
            contentTypePaths.put("application/pdf", documentsDir);
            contentTypePaths.put("audio/mpeg", audioDir);
            contentTypePaths.put("video/mp4", videoDir);
            contentTypePaths.put("application/octet-stream", otherDir);
        } catch (IOException e) {
            fail("Failed to create test directories: " + e.getMessage());
        }

        when(fileStorageProperties.getUploadDir()).thenReturn(tempDir.toString());
        when(fileStorageProperties.isContentTypeAllowed(any())).thenReturn(true);
        when(fileStorageProperties.getMaxFileSize()).thenReturn(1073741824L); // 1GB

        // Mock the getPathForContentType method
        when(fileStorageProperties.getPathForContentType(any())).thenAnswer(invocation -> {
            String contentType = invocation.getArgument(0);
            return contentTypePaths.getOrDefault(contentType, tempDir.resolve("other"));
        });

        // Mock the getSubdirectoryForContentType method
        when(fileStorageProperties.getSubdirectoryForContentType(any())).thenAnswer(invocation -> {
            String contentType = invocation.getArgument(0);
            if (contentType.startsWith("image/")) return "images";
            if (contentType.startsWith("text/") || contentType.equals("application/pdf")) return "documents";
            if (contentType.startsWith("audio/")) return "audio";
            if (contentType.startsWith("video/")) return "video";
            return "other";
        });

        // Mock user repository to return a user
        User mockUser = new User();
        mockUser.setId(1L);
        when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

        // Mock fileMetadataService
        when(fileMetadataService.saveFileMetadata(any(FileMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fileChunkService = new FileChunkService(
            fileStorageProperties,
            fileMetadataService,
            userRepository,
            fileMetadataRepository
        );
    }

    @Test
    void processChunk_shouldReturnNullForFirstChunk() {
        // Arrange
        FileChunk chunk = createTestChunk(1, 2, "test.txt", "text/plain", "Hello");

        // Mock the findUploadIdForFirstChunk method
        String uploadId = fileChunkService.findUploadIdForFirstChunk(chunk, 1L);
        chunk.setUploadId(uploadId);

        // Act
        String result = fileChunkService.processChunk(chunk, 1L);

        // Assert
        assertNull(result, "Should return null for the first chunk when more chunks are expected");
    }

    @Test
    void processChunk_shouldReturnFilePathWhenAllChunksReceived() {
        // Arrange
        FileChunk chunk1 = createTestChunk(1, 2, "test.txt", "text/plain", "Hello");
        FileChunk chunk2 = createTestChunk(2, 2, "test.txt", "text/plain", " World");

        // Set the same uploadId for both chunks to ensure they're treated as part of the same upload
        String uploadId = fileChunkService.findUploadIdForFirstChunk(chunk1, 1L);
        chunk1.setUploadId(uploadId);
        chunk2.setUploadId(uploadId);

        // Act
        String result1 = fileChunkService.processChunk(chunk1, 1L);
        String result2 = fileChunkService.processChunk(chunk2, 1L);

        // Assert
        assertNull(result1, "Should return null for the first chunk");
        assertNotNull(result2, "Should return a file path when all chunks are received");

        // Verify the file exists and has the correct content
        File file = new File(result2);
        assertTrue(file.exists(), "File should exist");

        try {
            byte[] content = Files.readAllBytes(file.toPath());
            assertEquals("Hello World", new String(content), "File content should match the combined chunks");
        } catch (IOException e) {
            fail("Failed to read file: " + e.getMessage());
        }
    }

    @Test
    void findUploadIdForFirstChunk_shouldReturnUniqueId() {
        // Arrange
        FileChunk chunk = createTestChunk(1, 2, "test.txt", "text/plain", "Hello");

        // Act
        String uploadId = fileChunkService.findUploadIdForFirstChunk(chunk, 1L);

        // Assert
        assertNotNull(uploadId, "Should return a non-null upload ID");
        assertTrue(uploadId.length() > 0, "Upload ID should not be empty");
    }

    private FileChunk createTestChunk(int index, int total, String fileName, String contentType, String data) {
        return FileChunk.builder()
            .chunkIndex(index)
            .totalChunks(total)
            .fileName(fileName)
            .contentType(contentType)
            .data(Base64.getEncoder().encodeToString(data.getBytes()))
            .fileSize((long) data.length())
            .chatRoomId(1L)
            .build();
    }
}
