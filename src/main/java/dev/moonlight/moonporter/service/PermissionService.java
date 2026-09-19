package dev.moonlight.moonporter.service;

import dev.moonlight.moonporter.config.MoonPorterConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Все проверки прав плагина в одном месте.
 *
 * Правило одно: при выключенной системе прав разрешено всё,
 * а при включённой узел проверяется только если субъект не освобоён
 * настройкой settings.permissions.op_bypass (OP-игроки и консоль).
 */
public final class PermissionService {

    private final MoonPorterConfig config;

    public PermissionService(@NotNull MoonPorterConfig config) {
        this.config = config;
    }

    /**
     * Право брать и сдавать груз.
     *
     * @param player игрок
     * @return true если действие разрешено
     */
    public boolean hasUse(@NotNull Player player) {

        if (!config.arePermissionsEnabled()) {
            return true;
        }

        return isExempt(player) || player.hasPermission(config.getPermissionUse());

    }

    /**
     * Право административных команд плагина.
     *
     * @param sender отправитель команды
     * @return true если действие разрешено
     */
    public boolean hasAdmin(@NotNull CommandSender sender) {

        if (!config.arePermissionsEnabled()) {
            return true;
        }

        return isExempt(sender) || sender.hasPermission(config.getPermissionAdmin());

    }

    /**
     * Право обхода задержки между переносками.
     *
     * @param player игрок
     * @return true если кулдаун применять не нужно
     */
    public boolean hasCooldownBypass(@NotNull Player player) {
        return isExempt(player) || player.hasPermission(config.getPermissionBypassCooldown());
    }

    /**
     * Освобождён ли субъект от проверок настройкой op_bypass:
     * консоль и OP-игроки, когда флаг включён.
     *
     * @param sender субъект проверки
     * @return true если проверку можно пропустить
     */
    private boolean isExempt(@NotNull CommandSender sender) {

        if (!config.isOpBypassEnabled()) {
            return false;
        }

        return !(sender instanceof Player) || sender.isOp();

    }
}
