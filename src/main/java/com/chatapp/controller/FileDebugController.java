package com.chatapp.controller;

import com.chatapp.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class FileDebugController {

    private final FileStorageProperties fileStorageProperties;

    @GetMapping("/files/status")
    public ResponseEntity<Map<String, Object>> getFileSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        Path baseDir = Paths.get(fileStorageProperties.getUploadDir());

        // Check base upload directory
        status.put("baseDir", baseDir.toAbsolutePath().toString());
        status.put("baseDirExists", Files.exists(baseDir));
        status.put("baseDirWritable", Files.isWritable(baseDir));

        // Check subdirectories
        Map<String, Map<String, Object>> subdirs = new HashMap<>();
        String[] subDirNames = {"images", "documents", "audio", "video", "other"};

        for (String subDirName : subDirNames) {
            Path subDir = baseDir.resolve(subDirName);
            Map<String, Object> subDirStatus = new HashMap<>();
            subDirStatus.put("path", subDir.toAbsolutePath().toString());
            subDirStatus.put("exists", Files.exists(subDir));
            subDirStatus.put("writable", Files.isWritable(subDir));
            subdirs.put(subDirName, subDirStatus);
        }
        status.put("subdirectories", subdirs);

        // Add configuration info
        status.put("maxFileSize", fileStorageProperties.getMaxFileSize());
        status.put("allowedContentTypes", fileStorageProperties.getAllowedContentTypes());

        return ResponseEntity.ok(status);
    }
}
