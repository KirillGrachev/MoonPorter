package dev.moonlight.moonporter.config;

import dev.moonlight.moonporter.config.type.CargoMode;
import dev.moonlight.moonporter.config.type.DeliveryTrigger;
import dev.moonlight.moonporter.config.type.TitleType;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Контракт доступа к конфигурации MoonPorter.
 * Сервисы, реестры и слушатели зависят от этого интерфейса,
 * а не от конкретного ConfigManager.
 */
public interface MoonPorterConfig {

    /** Основные настройки */
    boolean isEnabled();

    @NotNull Material getMaterial();

    /* Груз */

    @NotNull CargoMode getCargoMode();

    boolean isCargoNameVisible();

    /* Нарушения */

    boolean isResetFlightEnabled();

    boolean isResetGamemodeEnabled();

    /** Титулы */
    boolean isTitleEnabled();

    int getTitleFadeIn();

    int getTitleStay();

    int getTitleFadeOut();

    /** Доставка */
    @NotNull DeliveryTrigger getDeliveryTrigger();

    long getDeliveryTimeoutMillis();

    double getDeliveryRadiusSquared();

    /**
     * Проверяет мир по белому списку settings.allowed_worlds.
     * Поиск по кэшированному множеству, O(1).
     *
     * @param worldName имя мира
     * @return true если переноска в мире разрешена
     */
    boolean isAllowedWorld(@NotNull String worldName);

    /** Кулдаун */
    boolean isCooldownEnabled();

    long getCooldownMillis();

    /** Права */
    boolean arePermissionsEnabled();

    /**
     * Освобождают ли OP-игроки и консоль от проверок прав.
     *
     * @return true если op_bypass включён
     */
    boolean isOpBypassEnabled();

    @NotNull String getPermissionAdmin();

    @NotNull String getPermissionUse();

    @NotNull String getPermissionBypassCooldown();

    /** Зоны действия */
    @NotNull List<Integer> getNpcIds();

    @NotNull List<String> getAllowedWorlds();

    @NotNull List<String> getAllowedRegions();

    /** Уровни груза */
    /**
     * Уровни, объявленные в settings.porters, в порядке следования.
     * Пустой список — реестр подставит встроенный набор по умолчанию.
     *
     * @return неизменяемый список уровней
     */
    @NotNull List<PorterTier> getPorterTiers();

    /** Сообщения */
    @NotNull String getPrefix();

    @NotNull TitleMessage getTitle(@NotNull TitleType type);

    @NotNull String getRewardFormat();

    @NotNull String getCommandNoPermissionMessage();

    @NotNull String getCommandPlayerOnlyMessage();

    @NotNull List<String> getCommandUsageMessage();

    @NotNull String getReloadSuccessMessage();

    @NotNull String getCommandNoTiersMessage();

    @NotNull List<String> getReloadReportMessage();

    /** Управление */
    void reload();
}
