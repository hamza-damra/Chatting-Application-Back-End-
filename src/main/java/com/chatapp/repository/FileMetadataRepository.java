package com.chatapp.repository;

import com.chatapp.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for file metadata operations
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /**
     * Find file metadata by file hash
     * @param fileHash MD5 hash of the file
     * @return Optional containing file metadata if found
     */
    Optional<FileMetadata> findByFileHash(String fileHash);

    /**
     * Find file metadata by filename
     * @param fileName The filename to search for
     * @return Optional containing file metadata if found
     */
    Optional<FileMetadata> findByFileName(String fileName);

    /**
     * Find all files with the same hash
     * @param fileHash MD5 hash of the file
     * @return List of file metadata with matching hash
     */
    List<FileMetadata> findAllByFileHash(String fileHash);

    /**
     * Find all files uploaded by a specific user
     * @param userId User ID
     * @return List of file metadata uploaded by the user
     */
    List<FileMetadata> findAllByUploadedById(Long userId);

    /**
     * Find all files of a specific content type
     * @param contentType MIME type of the file
     * @return List of file metadata with matching content type
     */
    List<FileMetadata> findAllByContentType(String contentType);

    /**
     * Find all files in a specific storage location
     * @param storageLocation Storage location (folder path)
     * @return List of file metadata in the specified location
     */
    List<FileMetadata> findAllByStorageLocation(String storageLocation);

    /**
     * Find all duplicate files
     * @return List of file metadata marked as duplicates
     */
    List<FileMetadata> findAllByIsDuplicateTrue();

    /**
     * Find all files with a specific content type prefix
     * @param contentTypePrefix Prefix of the content type (e.g., "image/")
     * @return List of file metadata with matching content type prefix
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.contentType LIKE :prefix%")
    List<FileMetadata> findAllByContentTypeStartingWith(@Param("prefix") String contentTypePrefix);
}
