package com.chatapp.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_chat_room", columnList = "chat_room_id"),
    @Index(name = "idx_message_sender", columnList = "sender_id"),
    @Index(name = "idx_message_sent_at", columnList = "sent_at")
})
@Getter
@Setter
@ToString(exclude = {"sender", "chatRoom", "messageStatuses", "fileMetadata"})
@EqualsAndHashCode(of = {"id", "content", "sentAt"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content; // Can be null for file-only messages

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @OneToMany(mappedBy = "message", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = true)
    @Builder.Default
    private List<MessageStatus> messageStatuses = new ArrayList<>();

    @Column(name = "content_type")
    private String contentType; // TEXT, IMAGE, FILE, etc.

    @Column(name = "attachment_url", length = 1024) // Increased length for long file paths
    private String attachmentUrl;

    @OneToOne
    @JoinColumn(name = "file_metadata_id")
    private FileMetadata fileMetadata;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    public void addMessageStatus(MessageStatus status) {
        messageStatuses.add(status);
        status.setMessage(this);
    }

    public void removeMessageStatus(MessageStatus status) {
        messageStatuses.remove(status);
        status.setMessage(null);
    }
    
    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
        if (fileMetadata != null && fileMetadata.getFilePath() != null) {
            this.attachmentUrl = fileMetadata.getFilePath();
        }
    }
}
