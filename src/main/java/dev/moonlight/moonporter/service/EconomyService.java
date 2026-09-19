package dev.moonlight.moonporter.service;

import dev.moonlight.moonporter.hook.VaultEconomyHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Обёртка над экономикой Vault.
 *
 * Сама не импортирует типы Vault: работает через VaultEconomyHook,
 * который создаётся только при установленном Vault. Без хука
 * плагин продолжает работать, награда просто не начисляется.
 */
public final class EconomyService {

    private final @Nullable VaultEconomyHook hook;

    public EconomyService(@Nullable VaultEconomyHook hook) {
        this.hook = hook;
    }

    /**
     * Подключена ли экономика.
     *
     * @return true если провайдер Vault доступен
     */
    public boolean isAvailable() {
        return hook != null;
    }

    /**
     * Начисляет игроку награду.
     *
     * @param player получатель
     * @param amount сумма
     * @return true если начисление прошло успешно
     */
    public boolean deposit(@NotNull Player player, int amount) {
        return hook != null && hook.deposit(player, amount);
    }
}
