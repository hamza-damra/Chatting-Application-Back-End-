package com.chatapp.repository;

import com.chatapp.model.NotificationPreferences;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {

    /**
     * Find notification preferences for a specific user
     */
    Optional<NotificationPreferences> findByUser(User user);

    /**
     * Find notification preferences by user ID
     */
    Optional<NotificationPreferences> findByUser_Id(Long userId);

    /**
     * Check if user has push notifications enabled
     */
    @Query("SELECT np.pushNotificationsEnabled FROM NotificationPreferences np WHERE np.user = :user")
    Optional<Boolean> isPushNotificationsEnabled(@Param("user") User user);

    /**
     * Check if user has do not disturb mode enabled
     */
    @Query("SELECT np.doNotDisturb FROM NotificationPreferences np WHERE np.user = :user")
    Optional<Boolean> isDoNotDisturbEnabled(@Param("user") User user);

    /**
     * Delete notification preferences for a user
     */
    void deleteByUser(User user);

    /**
     * Check if notification preferences exist for a user
     */
    boolean existsByUser(User user);
}
