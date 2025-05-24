package com.chatapp.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
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
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_is_online", columnList = "is_online")
})
@Getter
@Setter
@ToString(exclude = {"chatRooms", "sentMessages", "messageStatuses"})
@EqualsAndHashCode(of = {"id", "username", "email"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "is_online")
    private boolean isOnline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "user_chatrooms",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "chatroom_id")
    )
    @Builder.Default
    private Set<ChatRoom> chatRooms = new HashSet<>();

    @OneToMany(mappedBy = "sender", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    @Builder.Default
    private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    @Builder.Default
    private List<MessageStatus> messageStatuses = new ArrayList<>();

    @OneToMany(mappedBy = "uploadedBy", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    @Builder.Default
    private List<FileMetadata> uploadedFiles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastSeen = LocalDateTime.now();
    }

    // Helper methods for bidirectional relationships
    public void addSentMessage(Message message) {
        sentMessages.add(message);
        message.setSender(this);
    }

    public void removeSentMessage(Message message) {
        sentMessages.remove(message);
        if (message.getSender() == this) {
            message.setSender(null);
        }
    }

    public void addChatRoom(ChatRoom chatRoom) {
        chatRooms.add(chatRoom);
        chatRoom.getParticipants().add(this);
    }

    public void removeChatRoom(ChatRoom chatRoom) {
        chatRooms.remove(chatRoom);
        chatRoom.getParticipants().remove(this);
    }

    public void addMessageStatus(MessageStatus status) {
        messageStatuses.add(status);
        status.setUser(this);
    }

    public void removeMessageStatus(MessageStatus status) {
        messageStatuses.remove(status);
        if (status.getUser() == this) {
            status.setUser(null);
        }
    }

    public void addUploadedFile(FileMetadata fileMetadata) {
        uploadedFiles.add(fileMetadata);
        fileMetadata.setUploadedBy(this);
    }

    public void removeUploadedFile(FileMetadata fileMetadata) {
        uploadedFiles.remove(fileMetadata);
        if (fileMetadata.getUploadedBy() == this) {
            fileMetadata.setUploadedBy(null);
        }
    }
}
