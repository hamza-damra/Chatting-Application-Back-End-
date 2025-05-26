package com.chatapp.config;

import com.chatapp.controller.NotificationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that both notification controllers can coexist without bean name conflicts
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationBeanConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldHaveBothNotificationControllers() {
        // Verify REST controller exists
        assertTrue(applicationContext.containsBean("notificationController"));
        Object restController = applicationContext.getBean("notificationController");
        assertEquals(NotificationController.class, restController.getClass());

        // Verify WebSocket controller exists with custom name
        assertTrue(applicationContext.containsBean("webSocketNotificationController"));
        Object webSocketController = applicationContext.getBean("webSocketNotificationController");
        assertEquals(com.chatapp.websocket.NotificationController.class, webSocketController.getClass());

        // Verify they are different instances
        assertNotSame(restController, webSocketController);
    }

    @Test
    void shouldInjectRestControllerByDefault() {
        // When autowiring without qualifier, should get the REST controller
        NotificationController controller = applicationContext.getBean(NotificationController.class);
        assertNotNull(controller);
        assertEquals(NotificationController.class, controller.getClass());
    }

    @Test
    void shouldInjectWebSocketControllerWithQualifier() {
        // Should be able to get WebSocket controller with qualifier
        Object controller = applicationContext.getBean("webSocketNotificationController");
        assertNotNull(controller);
        assertEquals(com.chatapp.websocket.NotificationController.class, controller.getClass());
    }
}
