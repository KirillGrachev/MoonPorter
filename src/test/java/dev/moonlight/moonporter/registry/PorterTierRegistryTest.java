package dev.moonlight.moonporter.registry;

import dev.moonlight.moonporter.config.ConfigManager;
import dev.moonlight.moonporter.config.PorterTier;
import dev.moonlight.moonporter.config.TestConfigs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты реестра уровней груза: дефолты, конфиг, случайный выбор.
 */
class PorterTierRegistryTest {

    @Test
    @DisplayName("Пустой конфиг подставляет встроенный набор уровней")
    void registersDefaultTiersWhenConfigEmpty() {

        ConfigManager config = TestConfigs.empty();
        PorterTierRegistry registry = new PorterTierRegistry(config);

        assertEquals(3, registry.size());
        assertTrue(registry.isRegistered("low"));
        assertTrue(registry.isRegistered("normal"));
        assertTrue(registry.isRegistered("large"));

        PorterTier low = registry.getTier("low");

        assertNotNull(low);
        assertEquals(5, low.rewardMin());
        assertEquals(10, low.rewardMax());
        assertEquals(1, low.weight());

    }

    @Test
    @DisplayName("Ключ уровня не зависит от регистра")
    void resolvesTierCaseInsensitively() {

        PorterTierRegistry registry = new PorterTierRegistry(TestConfigs.empty());

        assertEquals(registry.getTier("low"), registry.getTier("LOW"));
        assertEquals(registry.getTier("large"), registry.getTier("Large"));

    }

    @Test
    @DisplayName("Конfig заменяет встроенный набор уровней")
    void usesConfiguredTiers() {

        ConfigManager config = TestConfigs.load("/test-config.yml");
        PorterTierRegistry registry = new PorterTierRegistry(config);

        assertEquals(2, registry.size());
        assertTrue(registry.isRegistered("test_low"));
        assertTrue(registry.isRegistered("test_high"));
        assertNull(registry.getTier("low"));

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
    @DisplayName("Перезагрузка реестра пересобирает уровни")
    void reloadRebuildsTiers() {

        PorterTierRegistry registry = new PorterTierRegistry(TestConfigs.empty());

        registry.reloadRegistry();

        assertEquals(3, registry.size());
        assertNotNull(registry.getTier("normal"));

    }

    @Test
    @DisplayName("Неизвестный ключ возвращает null")
    void returnsNullForUnknownTier() {
        assertNull(new PorterTierRegistry(TestConfigs.empty()).getTier("nope"));
    }
}
