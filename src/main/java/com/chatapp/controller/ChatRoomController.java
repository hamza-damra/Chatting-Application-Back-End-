package com.chatapp.controller;

import com.chatapp.dto.ChatRoomRequest;
import com.chatapp.dto.ChatRoomResponse;
import com.chatapp.dto.UserResponse;
import com.chatapp.exception.BadRequestException;
import com.chatapp.service.ChatRoomService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ChatRoomResponse>> getUserChatRooms() {
        List<ChatRoomResponse> chatRooms = chatRoomService.getCurrentUserChatRooms();
        return ResponseEntity.ok(chatRooms);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChatRoomResponse> getChatRoomById(@PathVariable Long id) {
        ChatRoomResponse chatRoom = chatRoomService.getChatRoomResponseById(id);
        return ResponseEntity.ok(chatRoom);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChatRoomResponse> createChatRoom(@Valid @RequestBody ChatRoomRequest request) {
        log.info("Creating chat room with request: name={}, isPrivate={}, participantIds={}",
                request.getName(), request.isPrivate(), request.getParticipantIds());
        ChatRoomResponse chatRoom = chatRoomService.createChatRoom(request);
        log.info("Created chat room response: id={}, name={}, isPrivate={}",
                chatRoom.getId(), chatRoom.getName(), chatRoom.isPrivate());
        return ResponseEntity.status(HttpStatus.CREATED).body(chatRoom);
    }

    @PostMapping("/private/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChatRoomResponse> createOrGetPrivateChat(@PathVariable Long userId) {
        ChatRoomResponse chatRoom = chatRoomService.createOrGetPrivateChat(userId);
        return ResponseEntity.ok(chatRoom);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChatRoomResponse> updateChatRoom(
            @PathVariable Long id,
            @Valid @RequestBody ChatRoomRequest request) {
        ChatRoomResponse chatRoom = chatRoomService.updateChatRoom(id, request);
        return ResponseEntity.ok(chatRoom);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long id) {
        chatRoomService.deleteChatRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UserResponse>> getChatRoomParticipants(@PathVariable Long id) {
        List<UserResponse> participants = chatRoomService.getChatRoomParticipants(id);
        return ResponseEntity.ok(participants);
    }

    @PostMapping("/{id}/participants/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> addParticipant(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "userId") Long userId) {
        // Additional validation to provide better error messages
        if (id == null) {
            throw new BadRequestException("Chat room ID cannot be null");
        }
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        chatRoomService.addParticipant(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/participants/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long id,
            @PathVariable Long userId) {
        // Additional validation to provide better error messages
        if (id == null) {
            throw new BadRequestException("Chat room ID cannot be null");
        }
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        chatRoomService.removeParticipant(id, userId);
        return ResponseEntity.noContent().build();
    }
}
