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
    private final PermissionService permissionService;

    public CooldownService(@NotNull MoonPorterConfig config,
                           @NotNull PermissionService permissionService) {
        this.config = config;
        this.permissionService = permissionService;
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

        return permissionService.hasCooldownBypass(player);

    }
}
