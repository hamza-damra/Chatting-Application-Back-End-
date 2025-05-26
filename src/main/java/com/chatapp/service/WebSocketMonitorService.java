package com.chatapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * Service to monitor WebSocket performance and connection statistics
 */
@Service
@Slf4j
public class WebSocketMonitorService {

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong totalDisconnections = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    
    private final Map<String, Long> userConnectionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> userMessageCounts = new ConcurrentHashMap<>();

    /**
     * Record a new WebSocket connection
     */
    public void recordConnection(String username) {
        activeConnections.incrementAndGet();
        totalConnections.incrementAndGet();
        userConnectionTimes.put(username, System.currentTimeMillis());
        
        log.debug("WEBSOCKET_MONITOR: User {} connected. Active connections: {}", 
            username, activeConnections.get());
    }

    /**
     * Record a WebSocket disconnection
     */
    public void recordDisconnection(String username) {
        activeConnections.decrementAndGet();
        totalDisconnections.incrementAndGet();
        
        Long connectionTime = userConnectionTimes.remove(username);
        if (connectionTime != null) {
            long sessionDuration = System.currentTimeMillis() - connectionTime;
            log.debug("WEBSOCKET_MONITOR: User {} disconnected after {} ms. Active connections: {}", 
                username, sessionDuration, activeConnections.get());
        }
        
        userMessageCounts.remove(username);
    }

    /**
     * Record a message sent via WebSocket
     */
    public void recordMessageSent(String destination) {
        messagesSent.incrementAndGet();
        
        if (log.isTraceEnabled()) {
            log.trace("WEBSOCKET_MONITOR: Message sent to {}. Total sent: {}", 
                destination, messagesSent.get());
        }
    }

    /**
     * Record a message received via WebSocket
     */
    public void recordMessageReceived(String username) {
        messagesReceived.incrementAndGet();
        userMessageCounts.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
        
        if (log.isTraceEnabled()) {
            log.trace("WEBSOCKET_MONITOR: Message received from {}. Total received: {}", 
                username, messagesReceived.get());
        }
    }

    /**
     * Get current WebSocket statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "activeConnections", activeConnections.get(),
            "totalConnections", totalConnections.get(),
            "totalDisconnections", totalDisconnections.get(),
            "messagesSent", messagesSent.get(),
            "messagesReceived", messagesReceived.get(),
            "connectedUsers", userConnectionTimes.keySet().size(),
            "averageMessagesPerUser", calculateAverageMessagesPerUser()
        );
    }

    /**
     * Get detailed connection information
     */
    public Map<String, Object> getDetailedStats() {
        Map<String, Object> stats = new java.util.HashMap<>(getStatistics());
        stats.put("userConnectionTimes", Map.copyOf(userConnectionTimes));
        stats.put("userMessageCounts", userMessageCounts.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get()
            )));
        return stats;
    }

    /**
     * Reset all statistics (for testing purposes)
     */
    public void resetStatistics() {
        activeConnections.set(0);
        totalConnections.set(0);
        totalDisconnections.set(0);
        messagesSent.set(0);
        messagesReceived.set(0);
        userConnectionTimes.clear();
        userMessageCounts.clear();
        
        log.info("WEBSOCKET_MONITOR: Statistics reset");
    }

    /**
     * Log current statistics (called periodically)
     */
    public void logStatistics() {
        Map<String, Object> stats = getStatistics();
        log.info("WEBSOCKET_MONITOR: Active: {}, Total Connections: {}, Messages Sent: {}, Messages Received: {}",
            stats.get("activeConnections"),
            stats.get("totalConnections"),
            stats.get("messagesSent"),
            stats.get("messagesReceived")
        );
    }

    /**
     * Check if the system is under high load
     */
    public boolean isHighLoad() {
        return activeConnections.get() > 100; // Configurable threshold
    }

    /**
     * Get connection health status
     */
    public String getHealthStatus() {
        int active = activeConnections.get();
        if (active == 0) {
            return "NO_CONNECTIONS";
        } else if (active < 10) {
            return "LOW_LOAD";
        } else if (active < 50) {
            return "NORMAL_LOAD";
        } else if (active < 100) {
            return "HIGH_LOAD";
        } else {
            return "VERY_HIGH_LOAD";
        }
    }

    private double calculateAverageMessagesPerUser() {
        if (userMessageCounts.isEmpty()) {
            return 0.0;
        }
        
        int totalMessages = userMessageCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        
        return (double) totalMessages / userMessageCounts.size();
    }
}
