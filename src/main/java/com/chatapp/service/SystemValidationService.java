package com.chatapp.service;

import com.chatapp.config.FileStorageProperties;
import com.chatapp.model.FileMetadata;
import com.chatapp.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for system validation and testing
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemValidationService {
    
    private final FileMetadataService fileMetadataService;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageProperties fileStorageProperties;
    
    /**
     * Run comprehensive system validation tests
     * @return Map containing test results
     */
    public Map<String, Object> validateSystem() {
        Map<String, Object> results = new HashMap<>();
        List<String> validationMessages = new ArrayList<>();
        
        try {
            // 1. Validate folder structure
            validationMessages.add(validateFolderStructure());
            
            // 2. Validate database integrity
            validationMessages.add(validateDatabaseIntegrity());
            
            // 3. Validate file type routing
            validationMessages.add(validateFileTypeRouting());
            
            // 4. Validate duplicate detection
            validationMessages.add(validateDuplicateDetection());
            
            // Compile statistics
            results.put("totalFiles", fileMetadataRepository.count());
            results.put("duplicateFiles", fileMetadataRepository.findAllByIsDuplicateTrue().size());
            results.put("validationMessages", validationMessages);
            results.put("status", "success");
            
            return results;
        } catch (Exception e) {
            log.error("Validation failed", e);
            results.put("status", "error");
            results.put("error", e.getMessage());
            return results;
        }
    }
    
    /**
     * Validate folder structure
     * @return Validation message
     */
    private String validateFolderStructure() {
        try {
            String[] requiredFolders = {"images", "documents", "audio", "video", "other", "temp"};
            List<String> missingFolders = new ArrayList<>();
            
            for (String folder : requiredFolders) {
                Path folderPath = Paths.get(fileStorageProperties.getUploadDir(), folder);
                if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                    missingFolders.add(folder);
                }
            }
            
            if (missingFolders.isEmpty()) {
                return "Folder structure validation: PASSED - All required folders exist";
            } else {
                return "Folder structure validation: FAILED - Missing folders: " + String.join(", ", missingFolders);
            }
        } catch (Exception e) {
            log.error("Error validating folder structure", e);
            return "Folder structure validation: ERROR - " + e.getMessage();
        }
    }
    
    /**
     * Validate database integrity
     * @return Validation message
     */
    private String validateDatabaseIntegrity() {
        try {
            List<FileMetadata> allFiles = fileMetadataRepository.findAll();
            int invalidFiles = 0;
            
            for (FileMetadata file : allFiles) {
                // Check if file exists on disk (unless it's marked as removed)
                if (!file.getFilePath().endsWith(".removed")) {
                    File diskFile = new File(file.getFilePath());
                    if (!diskFile.exists()) {
                        invalidFiles++;
                        log.warn("File in database but not on disk: {}", file.getFilePath());
                    }
                }
            }
            
            if (invalidFiles == 0) {
                return "Database integrity validation: PASSED - All database entries match files on disk";
            } else {
                return "Database integrity validation: WARNING - Found " + invalidFiles + 
                       " files in database that don't exist on disk";
            }
        } catch (Exception e) {
            log.error("Error validating database integrity", e);
            return "Database integrity validation: ERROR - " + e.getMessage();
        }
    }
    
    /**
     * Validate file type routing
     * @return Validation message
     */
    private String validateFileTypeRouting() {
        try {
            List<FileMetadata> allFiles = fileMetadataRepository.findAll();
            int misroutedFiles = 0;
            
            for (FileMetadata file : allFiles) {
                // Skip removed files
                if (file.getFilePath().endsWith(".removed")) {
                    continue;
                }
                
                String contentType = file.getContentType();
                String expectedLocation = fileStorageProperties.getSubdirectoryForContentType(contentType);
                String actualLocation = file.getStorageLocation();
                
                if (!expectedLocation.equals(actualLocation)) {
                    misroutedFiles++;
                    log.warn("File misrouted: {} - Content type: {}, Expected location: {}, Actual location: {}", 
                            file.getFilePath(), contentType, expectedLocation, actualLocation);
                }
            }
            
            if (misroutedFiles == 0) {
                return "File type routing validation: PASSED - All files are in correct folders";
            } else {
                return "File type routing validation: WARNING - Found " + misroutedFiles + 
                       " files in incorrect folders";
            }
        } catch (Exception e) {
            log.error("Error validating file type routing", e);
            return "File type routing validation: ERROR - " + e.getMessage();
        }
    }
    
    /**
     * Validate duplicate detection
     * @return Validation message
     */
    private String validateDuplicateDetection() {
        try {
            // Run duplicate scan
            fileMetadataService.scanForDuplicates();
            
            // Check if all duplicates are properly marked
            List<FileMetadata> allFiles = fileMetadataRepository.findAll();
            Map<String, List<FileMetadata>> filesByHash = new HashMap<>();
            
            for (FileMetadata file : allFiles) {
                filesByHash.computeIfAbsent(file.getFileHash(), k -> new ArrayList<>()).add(file);
            }
            
            int unmarkedDuplicates = 0;
            for (List<FileMetadata> filesWithSameHash : filesByHash.values()) {
                if (filesWithSameHash.size() > 1) {
                    // First file should be original, rest should be marked as duplicates
                    for (int i = 1; i < filesWithSameHash.size(); i++) {
                        if (!Boolean.TRUE.equals(filesWithSameHash.get(i).getIsDuplicate())) {
                            unmarkedDuplicates++;
                        }
                    }
                }
            }
            
            if (unmarkedDuplicates == 0) {
                return "Duplicate detection validation: PASSED - All duplicates properly marked";
            } else {
                return "Duplicate detection validation: WARNING - Found " + unmarkedDuplicates + 
                       " unmarked duplicates";
            }
        } catch (Exception e) {
            log.error("Error validating duplicate detection", e);
            return "Duplicate detection validation: ERROR - " + e.getMessage();
        }
    }
}
