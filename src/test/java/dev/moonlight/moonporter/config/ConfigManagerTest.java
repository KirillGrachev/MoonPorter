package dev.moonlight.moonporter.config;

import dev.moonlight.moonporter.config.type.CargoVisualType;
import dev.moonlight.moonporter.config.type.DeliveryTrigger;
import dev.moonlight.moonporter.config.type.TitleType;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты разбора конфигурации на тестовых данных из test-resources.
 */
class ConfigManagerTest {

    @Test
    @DisplayName("Читает основные настройки из тестового конфига")
    void readsSettingsFromTestConfig() {

        MoonPorterConfig config = TestConfigs.load("/test-config.yml");

        assertFalse(config.isEnabled());
        assertEquals(Material.CHEST, config.getMaterial());

        assertTrue(config.isTitleEnabled());
        assertEquals(5, config.getTitleFadeIn());
        assertEquals(50, config.getTitleStay());
        assertEquals(7, config.getTitleFadeOut());

        assertEquals(DeliveryTrigger.SNEAK_HOLD, config.getDeliveryTrigger());
        assertEquals(42_000L, config.getDeliveryTimeoutMillis());
        assertEquals(12.25D, config.getDeliveryRadiusSquared());

        assertTrue(config.isCooldownEnabled());
        assertEquals(45_000L, config.getCooldownMillis());

    }

    @Test
    @DisplayName("Читает права, миры, регионы и NPC")
    void readsZonesAndPermissions() {

        MoonPorterConfig config = TestConfigs.load("/test-config.yml");

        assertTrue(config.arePermissionsEnabled());
        assertTrue(config.isOpBypassEnabled());
        assertEquals("test.admin", config.getPermissionAdmin());
        assertEquals("test.use", config.getPermissionUse());
        assertEquals("test.bypass", config.getPermissionBypassCooldown());

        assertEquals(List.of(45, 46), config.getNpcIds());
        assertEquals(List.of("test_world"), config.getAllowedWorlds());
        assertTrue(config.isAllowedWorld("test_world"));
        assertFalse(config.isAllowedWorld("world"));
        assertEquals(List.of("test_region"), config.getAllowedRegions());

    }

    @Test
    @DisplayName("Читает уровни груза в порядке объявления")
    void readsPorterTiers() {

        MoonPorterConfig config = TestConfigs.load("/test-config.yml");

        List<PorterTier> tiers = config.getPorterTiers();

        assertEquals(2, tiers.size());

        PorterTier low = tiers.get(0);

        assertEquals("test_low", low.id());
        assertEquals(1, low.rewardMin());
        assertEquals(2, low.rewardMax());
        assertEquals(1, low.weight());
        assertTrue(low.name().contains("Тестовый"));

        PorterTier high = tiers.get(1);

        assertEquals("test_high", high.id());
        assertEquals(30, high.rewardMin());
        assertEquals(40, high.rewardMax());
        assertEquals(3, high.weight());

    }

    @Test
    @DisplayName("Окрашивает сообщения и читает титулы")
    void readsAndColorsMessages() {

        MoonPorterConfig config = TestConfigs.load("/test-config.yml");

        // &-коды преобразованы в section sign
        assertTrue(config.getPrefix().contains("§8["));
        assertTrue(config.getCommandNoPermissionMessage().contains("§c"));

        assertEquals("{amount} монет", config.getRewardFormat());
        assertEquals("§aReloaded: {count}", config.getReloadSuccessMessage());
        assertEquals(3, config.getCommandUsageMessage().size());

        // отсутствующий ключ сообщения: пустая строка и warning в консоль,
        // никакого встроенного текста из кода
        assertEquals("", config.getCommandNoTiersMessage());

        // отчёт перезагрузки — список строк из конфига с плейсхолдерами
        assertEquals(1, config.getReloadReportMessage().size());

        TitleMessage success = config.getTitle(TitleType.PICKUP_SUCCESS);

        assertTrue(success.enabled());
        assertEquals("§aТест", success.title());
        assertEquals("§fВзят", success.subtitle());

        // enabled: false отключает титул целиком
        assertTrue(config.getTitle(TitleType.PICKUP_DENIED).isDisabled());

        // отсутствующая секция: титул отключён, в консоль уходит warning с путём
        assertTrue(config.getTitle(TitleType.FLIGHT).isDisabled());

        // секция есть, но тексты пустые: титул тоже отключён
        assertTrue(config.getTitle(TitleType.TIMEOUT).isDisabled());

    }

    @Test
    @DisplayName("Пустой конфиг даёт значения по умолчанию")
    void fallsBackToDefaultsOnEmptyConfig() {

        MoonPorterConfig config = TestConfigs.empty();

        assertTrue(config.isEnabled());
        assertEquals(Material.BARREL, config.getMaterial());

        assertEquals(CargoVisualType.HEAD, config.getCargoVisualType());
        assertTrue(config.isCargoNameVisible());
        assertEquals(0.50D, config.getHandsForward());
        assertEquals(1.15D, config.getHandsHeight());
        assertTrue(config.isResetFlightEnabled());
        assertTrue(config.isResetGamemodeEnabled());

        assertEquals(DeliveryTrigger.SNEAK_TOGGLE, config.getDeliveryTrigger());
        assertEquals(15_000L, config.getDeliveryTimeoutMillis());
        assertEquals(0.0D, config.getDeliveryRadiusSquared());

        // пустой конфиг: отчёта нет, в консоль уходит warning об отсутствующей секции
        assertTrue(config.getReloadReportMessage().isEmpty());

        assertFalse(config.isCooldownEnabled());
        assertEquals(30_000L, config.getCooldownMillis());

        assertFalse(config.arePermissionsEnabled());
        assertFalse(config.isOpBypassEnabled());
        assertEquals("moonporter.admin", config.getPermissionAdmin());

        assertEquals(20, config.getTitleFadeIn());
        assertEquals(40, config.getTitleStay());
        assertEquals(20, config.getTitleFadeOut());

        assertTrue(config.getPorterTiers().isEmpty());

    }
}
