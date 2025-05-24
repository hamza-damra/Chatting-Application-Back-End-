package com.chatapp.repository;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomOrderBySentAtDesc(ChatRoom chatRoom);

    Page<Message> findByChatRoomOrderBySentAtDesc(ChatRoom chatRoom, Pageable pageable);

    List<Message> findBySenderAndSentAtAfter(User sender, LocalDateTime after);

    @Query("SELECT m FROM Message m WHERE m.chatRoom = :chatRoom AND m.sentAt > :timestamp ORDER BY m.sentAt ASC")
    List<Message> findNewMessagesInChatRoom(@Param("chatRoom") ChatRoom chatRoom, @Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom = :chatRoom AND m.sentAt > :timestamp")
    long countNewMessagesInChatRoom(@Param("chatRoom") ChatRoom chatRoom, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find the most recent message in a chat room
     * @param chatRoom The chat room to find the most recent message for
     * @return List containing the most recent message(s), or empty list if no messages exist
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoom = :chatRoom ORDER BY m.sentAt DESC")
    List<Message> findMostRecentMessagesInChatRoom(@Param("chatRoom") ChatRoom chatRoom, Pageable pageable);

    /**
     * Find the most recent message in a chat room
     * @param chatRoomId The ID of the chat room to find the most recent message for
     * @return List containing the most recent message(s), or empty list if no messages exist
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.sentAt DESC")
    List<Message> findMostRecentMessagesByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);
}
