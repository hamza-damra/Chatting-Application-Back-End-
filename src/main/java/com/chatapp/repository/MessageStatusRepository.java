package com.chatapp.repository;

import com.chatapp.model.Message;
import com.chatapp.model.MessageStatus;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {

    List<MessageStatus> findByMessage(Message message);

    List<MessageStatus> findByUser(User user);

    Optional<MessageStatus> findByMessageAndUser(Message message, User user);

    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message = :message AND ms.status = :status")
    List<MessageStatus> findByMessageAndStatus(@Param("message") Message message, @Param("status") MessageStatus.Status status);

    @Query("SELECT COUNT(ms) FROM MessageStatus ms WHERE ms.message = :message AND ms.status = :status")
    long countByMessageAndStatus(@Param("message") Message message, @Param("status") MessageStatus.Status status);

    /**
     * Batch fetch message statuses for multiple messages and a specific user
     * This helps avoid N+1 query problems when loading message lists
     */
    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message IN :messages AND ms.user = :user")
    List<MessageStatus> findByMessagesAndUser(@Param("messages") List<Message> messages, @Param("user") User user);

    /**
     * Batch fetch all message statuses for multiple messages
     * This helps avoid N+1 query problems when loading message lists for senders
     */
    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message IN :messages")
    List<MessageStatus> findByMessages(@Param("messages") List<Message> messages);
}
