package com.chatapp.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a blocked user relationship
 * When user A blocks user B, they cannot send messages to each other
 */
@Entity
@Table(name = "blocked_users", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"}),
       indexes = {
           @Index(name = "idx_blocked_users_blocker", columnList = "blocker_id"),
           @Index(name = "idx_blocked_users_blocked", columnList = "blocked_id"),
           @Index(name = "idx_blocked_users_created", columnList = "created_at")
       })
@Getter
@Setter
@ToString(exclude = {"blocker", "blocked"})
@EqualsAndHashCode(of = {"id"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who initiated the block
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    /**
     * The user who is being blocked
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    /**
     * When the block was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Optional reason for blocking
     */
    @Column(length = 500)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Check if this block involves the given users
     */
    public boolean involvesUsers(User user1, User user2) {
        return (blocker.equals(user1) && blocked.equals(user2)) ||
               (blocker.equals(user2) && blocked.equals(user1));
    }

    /**
     * Check if user1 has blocked user2
     */
    public boolean isUserBlocked(User user1, User user2) {
        return blocker.equals(user1) && blocked.equals(user2);
    }
}
