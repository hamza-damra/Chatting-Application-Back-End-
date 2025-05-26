package com.chatapp.repository;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByParticipantsContaining(User user);

    @Query("SELECT c FROM ChatRoom c WHERE c.privateFlag = true AND :user1 MEMBER OF c.participants AND :user2 MEMBER OF c.participants AND SIZE(c.participants) = 2")
    Optional<ChatRoom> findPrivateChatBetween(@Param("user1") User user1, @Param("user2") User user2);

    List<ChatRoom> findByPrivateFlag(boolean privateFlag);

    @Query("SELECT c FROM ChatRoom c WHERE :user MEMBER OF c.participants")
    List<ChatRoom> findAllChatRoomsForUser(@Param("user") User user);
}
