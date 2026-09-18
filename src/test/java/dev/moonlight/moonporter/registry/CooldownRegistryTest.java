package dev.moonlight.moonporter.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты реестра задержек: чистая логика на системном времени.
 */
class CooldownRegistryTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    private CooldownRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CooldownRegistry();
    }

    @Test
    @DisplayName("Пустой реестр не содержит задержек")
    void isEmptyByDefault() {

        assertFalse(registry.isActive(PLAYER_ID));
        assertEquals(0L, registry.getRemainingSeconds(PLAYER_ID));

    }

    @Test
    @DisplayName("Задержка нулевой длительности не назначается")
    void ignoresZeroDuration() {

        registry.start(PLAYER_ID, 0);

        assertFalse(registry.isActive(PLAYER_ID));

    }

    @Test
    @DisplayName("Назначенная задержка активна и показывает остаток")
    void tracksActiveCooldown() {

        registry.start(PLAYER_ID, 10);

        assertTrue(registry.isActive(PLAYER_ID));
        assertTrue(registry.getRemainingSeconds(PLAYER_ID) > 0L);
        assertTrue(registry.getRemainingSeconds(PLAYER_ID) <= 10L);

    }

    @Test
    @DisplayName("Удаление снимает задержку")
    void removesCooldown() {

        registry.start(PLAYER_ID, 10);
        registry.remove(PLAYER_ID);

        assertFalse(registry.isActive(PLAYER_ID));

    }

    @Test
    @DisplayName("Очистка снимает все задержки")
    void clearsAllCooldowns() {

        registry.start(PLAYER_ID, 10);
        registry.clear();

        assertFalse(registry.isActive(PLAYER_ID));

    }
}
