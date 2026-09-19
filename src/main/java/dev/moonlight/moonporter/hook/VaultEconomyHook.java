package dev.moonlight.moonporter.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Интеграция с экономикой Vault.
 *
 * Класс загружается только после проверки наличия Vault в HookRegistrar-стиле:
 * основной код плагина не ссылается на типы Vault напрямую, поэтому
 * его отсутствие не может вызвать NoClassDefFoundError.
 */
public final class VaultEconomyHook {

    private final Economy economy;

    private VaultEconomyHook(@NotNull Economy economy) {
        this.economy = economy;
    }

    /**
     * Подключает провайдер экономики Vault.
     * Вызывается только если плагин Vault установлен и включён.
     *
     * @param plugin плагин-владелец
     * @return хук либо null, если провайдер экономики не зарегистрирован
     */
    public static @Nullable VaultEconomyHook attach(@NotNull Plugin plugin) {

        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (registration == null || registration.getProvider() == null) {
            return null;
        }

        return new VaultEconomyHook(registration.getProvider());

    }

    /**
     * Начисляет игроку награду.
     *
     * @param player получатель
     * @param amount сумма
     * @return true если транзакция прошла успешно
     */
    public boolean deposit(@NotNull Player player, int amount) {

        if (amount <= 0) {
            return false;
        }

        EconomyResponse response = economy.depositPlayer(player, amount);

        return response != null && response.transactionSuccess();

    }
}
