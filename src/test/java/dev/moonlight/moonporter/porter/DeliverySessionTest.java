package dev.moonlight.moonporter.porter;

import dev.moonlight.moonporter.config.PorterTier;
import dev.moonlight.moonporter.porter.cargo.Cargo;
import dev.moonlight.moonporter.porter.cargo.CargoVisual;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты чистой логики сессии переноски.
 * Bukkit-объекты не создаются — проверяется только расчёт времени и данные груза.
 */
class DeliverySessionTest {

    private static final PorterTier TEST_TIER = new PorterTier("low", "Груз", 5, 10, 1);

    @Test
    @DisplayName("Сессия не истекла до момента expiresAt")
    void isNotExpiredBeforeDeadline() {
        assertFalse(session(1000L).isExpired(999L));
    }

    @Test
    @DisplayName("Сессия истекает ровно в expiresAt и позже")
    void expiresAtDeadline() {

        DeliverySession session = session(1000L);

        assertTrue(session.isExpired(1000L));
        assertTrue(session.isExpired(1500L));

    }

    @Test
    @DisplayName("Сессия хранит данные груза без изменений")
    void keepsCargoData() {

        Cargo cargo = session(1000L).cargo();

        assertEquals(Material.BARREL, cargo.material());
        assertEquals("low", cargo.tier().id());
        assertEquals(5, cargo.tier().rewardMin());
        assertEquals(10, cargo.tier().rewardMax());
        assertEquals(1, cargo.tier().weight());
        assertEquals("Груз", cargo.displayName());

    }

    private @NotNull DeliverySession session(long expiresAt) {

        return new DeliverySession(
                UUID.randomUUID(),
                new Cargo(Material.BARREL, TEST_TIER, "Груз", null),
                new NoOpVisual(),
                expiresAt
        );

    }

    /** Визуализация-заглушка: не обращается к миру */
    private static final class NoOpVisual implements CargoVisual {

        @Override
        public void attach(@NotNull Player player) {
        }

        @Override
        public void remove() {
        }

        @Override
        public boolean isAlive() {
            return false;
        }
    }
}
