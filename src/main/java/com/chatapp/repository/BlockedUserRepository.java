package com.chatapp.repository;

import com.chatapp.model.BlockedUser;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    /**
     * Find a specific block relationship between two users
     */
    @Query("SELECT bu FROM BlockedUser bu WHERE bu.blocker = :blocker AND bu.blocked = :blocked")
    Optional<BlockedUser> findByBlockerAndBlocked(@Param("blocker") User blocker, @Param("blocked") User blocked);

    /**
     * Check if user1 has blocked user2
     */
    @Query("SELECT COUNT(bu) > 0 FROM BlockedUser bu WHERE bu.blocker = :blocker AND bu.blocked = :blocked")
    boolean isUserBlocked(@Param("blocker") User blocker, @Param("blocked") User blocked);

    /**
     * Check if there's any block relationship between two users (either direction)
     */
    @Query("SELECT COUNT(bu) > 0 FROM BlockedUser bu WHERE " +
           "(bu.blocker = :user1 AND bu.blocked = :user2) OR " +
           "(bu.blocker = :user2 AND bu.blocked = :user1)")
    boolean isBlocked(@Param("user1") User user1, @Param("user2") User user2);

    /**
     * Get all users blocked by a specific user
     */
    @Query("SELECT bu.blocked FROM BlockedUser bu WHERE bu.blocker = :blocker ORDER BY bu.createdAt DESC")
    List<User> findBlockedUsersByBlocker(@Param("blocker") User blocker);

    /**
     * Get all users who have blocked a specific user
     */
    @Query("SELECT bu.blocker FROM BlockedUser bu WHERE bu.blocked = :blocked ORDER BY bu.createdAt DESC")
    List<User> findBlockersByBlocked(@Param("blocked") User blocked);

    /**
     * Get all block relationships for a user (both as blocker and blocked)
     */
    @Query("SELECT bu FROM BlockedUser bu WHERE bu.blocker = :user OR bu.blocked = :user ORDER BY bu.createdAt DESC")
    List<BlockedUser> findAllBlockRelationshipsForUser(@Param("user") User user);

    /**
     * Get all block relationships where user is the blocker
     */
    @Query("SELECT bu FROM BlockedUser bu WHERE bu.blocker = :blocker ORDER BY bu.createdAt DESC")
    List<BlockedUser> findByBlocker(@Param("blocker") User blocker);

    /**
     * Count how many users a specific user has blocked
     */
    @Query("SELECT COUNT(bu) FROM BlockedUser bu WHERE bu.blocker = :blocker")
    long countBlockedUsers(@Param("blocker") User blocker);

    /**
     * Count how many users have blocked a specific user
     */
    @Query("SELECT COUNT(bu) FROM BlockedUser bu WHERE bu.blocked = :blocked")
    long countBlockers(@Param("blocked") User blocked);

    /**
     * Delete block relationship between two users
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedUser bu WHERE bu.blocker = :blocker AND bu.blocked = :blocked")
    void deleteByBlockerAndBlocked(@Param("blocker") User blocker, @Param("blocked") User blocked);

    /**
     * Delete all block relationships for a user (when user is deleted)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedUser bu WHERE bu.blocker = :user OR bu.blocked = :user")
    void deleteAllByUser(@Param("user") User user);
}
