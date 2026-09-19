package dev.moonlight.moonporter.registry;

import dev.moonlight.moonporter.config.ConfigManager;
import dev.moonlight.moonporter.config.TestConfigs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты реестра уровней груза: пустая секция, конфиг, случайный выбор.
 * Встроенных уровней нет: пустой конфиг означает пустой реестр.
 */
class PorterTierRegistryTest {

    @Test
    @DisplayName("Пустой конфиг оставляет реестр пустым")
    void staysEmptyWhenConfigEmpty() {

        ConfigManager config = TestConfigs.empty();
        PorterTierRegistry registry = new PorterTierRegistry(config);

        assertEquals(0, registry.size());
        assertNull(registry.getTier("low"));
        assertNull(registry.getRandomTier());
        assertFalse(registry.isRegistered("low"));

    }

    @Test
    @DisplayName("Конfig наполняет реестр уровнями в порядке объявления")
    void usesConfiguredTiers() {

        ConfigManager config = TestConfigs.load("/test-config.yml");
        PorterTierRegistry registry = new PorterTierRegistry(config);

        assertEquals(2, registry.size());
        assertTrue(registry.isRegistered("test_low"));
        assertTrue(registry.isRegistered("test_high"));

        assertNotNull(registry.getTier("test_low"));
        assertEquals(1, registry.getTier("test_low").rewardMin());
        assertEquals(3, registry.getTier("test_high").weight());

    }

    @Test
    @DisplayName("Ключ уровня не зависит от регистра")
    void resolvesTierCaseInsensitively() {

        PorterTierRegistry registry = new PorterTierRegistry(TestConfigs.load("/test-config.yml"));

        assertEquals(registry.getTier("test_low"), registry.getTier("TEST_LOW"));
        assertEquals(registry.getTier("test_high"), registry.getTier("Test_High"));

    }

    @Test
    @DisplayName("Случайный уровень всегда из зарегистрированных")
    void returnsRandomRegisteredTier() {

        PorterTierRegistry registry = new PorterTierRegistry(TestConfigs.load("/test-config.yml"));

        for (int i = 0; i < 200; i++) {
            assertTrue(registry.isRegistered(registry.getRandomTier().id()));
        }

    }

    @Test
    @DisplayName("Перезагрузка реестра пересобирает уровни из конфига")
    void reloadRebuildsTiers() {

        PorterTierRegistry registry = new PorterTierRegistry(TestConfigs.load("/test-config.yml"));

        registry.reloadRegistry();

        assertEquals(2, registry.size());
        assertNotNull(registry.getTier("test_high"));

    }

    @Test
    @DisplayName("Неизвестный ключ возвращает null")
    void returnsNullForUnknownTier() {

        PorterTierRegistry registry = new PorterTierRegistry(TestConfigs.load("/test-config.yml"));

        assertNull(registry.getTier("nope"));

    }
}
