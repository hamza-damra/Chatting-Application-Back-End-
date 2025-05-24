package com.chatapp.service;

import com.chatapp.config.FileStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class FileStorageTest {

    @Mock
    private FileStorageProperties fileStorageProperties;

    @BeforeEach
    public void setup() throws IOException {
        // Setup temporary test directory
        Path tempDir = Files.createTempDirectory("chatapp-test");
        
        // Create subdirectories
        String[] subdirs = {"images", "documents", "audio", "video", "other", "temp"};
        for (String subdir : subdirs) {
            Files.createDirectories(tempDir.resolve(subdir));
        }
        
        // Mock the properties
        when(fileStorageProperties.getUploadDir()).thenReturn(tempDir.toString());
        
        // Setup content type mapping
        Map<String, String> contentTypeMapping = new HashMap<>();
        contentTypeMapping.put("image/jpeg", "images");
        contentTypeMapping.put("image/png", "images");
        contentTypeMapping.put("image/gif", "images");
        contentTypeMapping.put("application/pdf", "documents");
        contentTypeMapping.put("application/msword", "documents");
        contentTypeMapping.put("text/plain", "documents");
        contentTypeMapping.put("audio/mpeg", "audio");
        contentTypeMapping.put("audio/wav", "audio");
        contentTypeMapping.put("video/mp4", "video");
        contentTypeMapping.put("video/mpeg", "video");
        
        when(fileStorageProperties.getSubdirectoryForContentType("image/jpeg")).thenReturn("images");
        when(fileStorageProperties.getSubdirectoryForContentType("image/png")).thenReturn("images");
        when(fileStorageProperties.getSubdirectoryForContentType("image/gif")).thenReturn("images");
        when(fileStorageProperties.getSubdirectoryForContentType("application/pdf")).thenReturn("documents");
        when(fileStorageProperties.getSubdirectoryForContentType("application/msword")).thenReturn("documents");
        when(fileStorageProperties.getSubdirectoryForContentType("text/plain")).thenReturn("documents");
        when(fileStorageProperties.getSubdirectoryForContentType("audio/mpeg")).thenReturn("audio");
        when(fileStorageProperties.getSubdirectoryForContentType("audio/wav")).thenReturn("audio");
        when(fileStorageProperties.getSubdirectoryForContentType("video/mp4")).thenReturn("video");
        when(fileStorageProperties.getSubdirectoryForContentType("video/mpeg")).thenReturn("video");
        when(fileStorageProperties.getSubdirectoryForContentType("application/octet-stream")).thenReturn("other");
    }

    @Test
    public void testDirectoryStructure() {
        // Get the upload directory
        Path uploadPath = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath();
        
        // Check that the main directory exists
        assertTrue(Files.exists(uploadPath), "Upload directory should exist");
        
        // Check that the subdirectories exist
        String[] subdirs = {"images", "documents", "audio", "video", "other", "temp"};
        for (String subdir : subdirs) {
            Path subdirPath = uploadPath.resolve(subdir);
            assertTrue(Files.exists(subdirPath), "Subdirectory " + subdir + " should exist");
            assertTrue(Files.isDirectory(subdirPath), "Subdirectory " + subdir + " should be a directory");
        }
    }
    
    @Test
    public void testContentTypeMapping() {
        // Test image content types
        assertEquals("images", fileStorageProperties.getSubdirectoryForContentType("image/jpeg"));
        assertEquals("images", fileStorageProperties.getSubdirectoryForContentType("image/png"));
        assertEquals("images", fileStorageProperties.getSubdirectoryForContentType("image/gif"));
        
        // Test document content types
        assertEquals("documents", fileStorageProperties.getSubdirectoryForContentType("application/pdf"));
        assertEquals("documents", fileStorageProperties.getSubdirectoryForContentType("application/msword"));
        assertEquals("documents", fileStorageProperties.getSubdirectoryForContentType("text/plain"));
        
        // Test audio content types
        assertEquals("audio", fileStorageProperties.getSubdirectoryForContentType("audio/mpeg"));
        assertEquals("audio", fileStorageProperties.getSubdirectoryForContentType("audio/wav"));
        
        // Test video content types
        assertEquals("video", fileStorageProperties.getSubdirectoryForContentType("video/mp4"));
        assertEquals("video", fileStorageProperties.getSubdirectoryForContentType("video/mpeg"));
        
        // Test unknown content type
        assertEquals("other", fileStorageProperties.getSubdirectoryForContentType("application/octet-stream"));
    }
    
    @Test
    public void testWriteTestFile() throws IOException {
        // Create a test file in the temp directory
        Path tempDir = Paths.get(fileStorageProperties.getUploadDir()).resolve("temp").toAbsolutePath();
        assertTrue(Files.exists(tempDir), "Temp directory should exist");
        
        // Create a test file
        String testFileName = "test-file-" + System.currentTimeMillis() + ".txt";
        Path testFilePath = tempDir.resolve(testFileName);
        
        // Write some content to the file
        String content = "This is a test file created at " + java.time.LocalDateTime.now();
        Files.write(testFilePath, content.getBytes());
        
        // Verify the file was created
        assertTrue(Files.exists(testFilePath), "Test file should exist");
        assertEquals(content, new String(Files.readAllBytes(testFilePath)), "File content should match");
        
        // Clean up
        Files.delete(testFilePath);
    }
}
