package com.chatapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for storing file metadata
 */
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_file_storage_location", columnList = "storage_location"),
    @Index(name = "idx_file_uploaded_by", columnList = "user_id"),
    @Index(name = "idx_file_hash", columnList = "file_hash"),
    @Index(name = "idx_file_content_type", columnList = "content_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"uploadedBy", "message"})
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "content_type", nullable = false)
    private String contentType;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "file_hash", nullable = false)
    private String fileHash;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User uploadedBy;
    
    @Column(name = "storage_location", nullable = false)
    private String storageLocation;
    
    @Column(name = "is_duplicate")
    private Boolean isDuplicate;
    
    @Column(name = "original_file_id")
    private Long originalFileId;
    
    @OneToOne(mappedBy = "fileMetadata")
    private Message message;
    
    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (isDuplicate == null) {
            isDuplicate = false;
        }
    }
}
