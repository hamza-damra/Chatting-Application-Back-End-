package com.chatapp.controller;

import com.chatapp.model.FileMetadata;
import com.chatapp.service.FileMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for file management operations
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileManagementController {
    
    private final FileMetadataService fileMetadataService;
    
    /**
     * Get all file metadata
     * @return List of all file metadata
     */
    @GetMapping
    public ResponseEntity<List<FileMetadata>> getAllFiles() {
        return ResponseEntity.ok(fileMetadataService.findAllFiles());
    }
    
    /**
     * Scan for duplicate files
     * @return Number of duplicates found
     */
    @PostMapping("/scan-duplicates")
    public ResponseEntity<Map<String, Object>> scanForDuplicates() {
        int duplicatesFound = fileMetadataService.scanForDuplicates();
        
        Map<String, Object> response = new HashMap<>();
        response.put("duplicatesFound", duplicatesFound);
        response.put("message", "Scanned for duplicates and found " + duplicatesFound + " duplicate files");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Remove duplicate files
     * @return Number of files removed
     */
    @DeleteMapping("/duplicates")
    public ResponseEntity<Map<String, Object>> removeDuplicateFiles() {
        int removedCount = fileMetadataService.removeDuplicateFiles();
        
        Map<String, Object> response = new HashMap<>();
        response.put("removedCount", removedCount);
        response.put("message", "Removed " + removedCount + " duplicate files");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all duplicate files
     * @return List of duplicate file metadata
     */
    @GetMapping("/duplicates")
    public ResponseEntity<List<FileMetadata>> getDuplicateFiles() {
        return ResponseEntity.ok(fileMetadataService.findAllDuplicates());
    }
    
    /**
     * Scan filesystem for files not in the database
     * @return Number of files added to the database
     */
    @PostMapping("/scan-filesystem")
    public ResponseEntity<Map<String, Object>> scanFilesystem() {
        int filesAdded = fileMetadataService.scanFilesystem();
        
        Map<String, Object> response = new HashMap<>();
        response.put("filesAdded", filesAdded);
        response.put("message", "Scanned filesystem and added " + filesAdded + " files to the database");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Run validation tests
     * @return Test results
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSystem() {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Count files by type
            Map<String, Integer> filesByType = new HashMap<>();
            List<FileMetadata> allFiles = fileMetadataService.findAllFiles();
            
            for (FileMetadata file : allFiles) {
                String type = file.getContentType().split("/")[0];
                filesByType.put(type, filesByType.getOrDefault(type, 0) + 1);
            }
            
            // Count files by storage location
            Map<String, Integer> filesByLocation = new HashMap<>();
            for (FileMetadata file : allFiles) {
                filesByLocation.put(file.getStorageLocation(), 
                                   filesByLocation.getOrDefault(file.getStorageLocation(), 0) + 1);
            }
            
            // Count duplicates
            int duplicateCount = fileMetadataService.findAllDuplicates().size();
            
            results.put("status", "success");
            results.put("totalFiles", allFiles.size());
            results.put("filesByType", filesByType);
            results.put("filesByLocation", filesByLocation);
            results.put("duplicateCount", duplicateCount);
            results.put("message", "System validation completed successfully");
            
            log.info("System validation results: {}", results);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error validating system", e);
            
            results.put("status", "error");
            results.put("message", "Error validating system: " + e.getMessage());
            
            return ResponseEntity.status(500).body(results);
        }
    }
}
