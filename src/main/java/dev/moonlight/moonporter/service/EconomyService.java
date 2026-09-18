package dev.moonlight.moonporter.service;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Обёртка над экономикой Vault.
 *
 * Плагин не падает без Vault: при отсутствии провайдера
 * выдача награды просто отключается, а перевозка продолжает работать.
 */
public final class EconomyService {

    private final @Nullable Economy economy;

    public EconomyService(@Nullable Economy economy) {
        this.economy = economy;
    }

    /**
     * Подключена ли экономика.
     *
     * @return true если провайдер Vault доступен
     */
    public boolean isAvailable() {
        return economy != null;
    }

    /**
     * Начисляет игроку награду.
     *
     * @param player получатель
     * @param amount сумма
     * @return true если начисление прошло успешно
     */
    public boolean deposit(@NotNull Player player, int amount) {

        if (economy == null || amount <= 0) {
            return false;
        }

        EconomyResponse response = economy.depositPlayer(player, amount);

        return response != null && response.transactionSuccess();

    }
}
