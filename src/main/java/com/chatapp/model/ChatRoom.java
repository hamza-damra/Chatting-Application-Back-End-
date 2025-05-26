package com.chatapp.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "chat_rooms", indexes = {
    @Index(name = "idx_chatroom_name", columnList = "name"),
    @Index(name = "idx_chatroom_is_private", columnList = "is_private"),
    @Index(name = "idx_chatroom_creator", columnList = "creator_id")
})
@Getter
@Setter
@ToString(exclude = {"participants", "messages"})
@EqualsAndHashCode(of = {"id", "name", "isPrivate"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToMany(mappedBy = "chatRooms")
    @Builder.Default
    private Set<User> participants = new HashSet<>();

    @OneToMany(mappedBy = "chatRoom", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isDirectMessage() {
        return isPrivate && participants.size() == 2;
    }

    public void addParticipant(User user) {
        participants.add(user);
        user.getChatRooms().add(this);
    }

    public void removeParticipant(User user) {
        participants.remove(user);
        user.getChatRooms().remove(this);
    }

    public void addMessage(Message message) {
        messages.add(message);
        message.setChatRoom(this);
    }

    public void removeMessage(Message message) {
        messages.remove(message);
        if (message.getChatRoom() == this) {
            message.setChatRoom(null);
        }
    }
}
