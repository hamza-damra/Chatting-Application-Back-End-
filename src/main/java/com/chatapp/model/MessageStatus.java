package com.chatapp.model;

import javax.persistence.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "message_statuses", indexes = {
    @Index(name = "idx_message_status_message", columnList = "message_id"),
    @Index(name = "idx_message_status_user", columnList = "user_id"),
    @Index(name = "idx_message_status_status", columnList = "status"),
    @Index(name = "idx_message_status_composite", columnList = "message_id,user_id", unique = true)
})
@Getter
@Setter
@ToString(exclude = {"message", "user"})
@EqualsAndHashCode(of = {"id", "status", "updatedAt"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        SENT,       // Message has been sent to the server
        DELIVERED,  // Message has been delivered to recipient's device
        READ        // Message has been read by the recipient
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
