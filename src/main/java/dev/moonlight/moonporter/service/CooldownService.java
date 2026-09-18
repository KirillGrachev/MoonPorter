package dev.moonlight.moonporter.service;

import dev.moonlight.moonporter.config.MoonPorterConfig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Проверка права обхода задержки между переносками.
 * При выключенной системе прав или выключенном кулдауне всегда разрешает.
 */
public final class CooldownService {

    private final MoonPorterConfig config;

    public CooldownService(@NotNull MoonPorterConfig config) {
        this.config = config;
    }

    /**
     * Проверяет, может ли игрок проигнорировать задержку.
     *
     * @param player проверяемый игрок
     * @return true если задержку применять не нужно
     */
    public boolean isBypassed(@NotNull Player player) {

        if (!config.isCooldownEnabled()) {
            return true;
        }

        if (!config.arePermissionsEnabled()) {
            return false;
        }

        return player.hasPermission(config.getPermissionBypassCooldown());

    }
}
