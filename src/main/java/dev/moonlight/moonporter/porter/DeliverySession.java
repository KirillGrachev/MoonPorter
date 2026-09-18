package dev.moonlight.moonporter.porter;

import dev.moonlight.moonporter.porter.cargo.Cargo;
import dev.moonlight.moonporter.porter.cargo.CargoVisual;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Активная сессия переноски груза.
 * Один экземпляр на игрока, живёт от выдачи груза до его сдачи или отмены.
 *
 * @param playerId  UUID несущего игрока
 * @param cargo     описание груза
 * @param visual    визуализация груза
 * @param expiresAt момент истечения времени доставки (System.currentTimeMillis)
 */
public record DeliverySession(@NotNull UUID playerId,
                              @NotNull Cargo cargo,
                              @NotNull CargoVisual visual,
                              long expiresAt) {

    /**
     * Проверяет истечение времени доставки.
     *
     * @param now текущее время в миллисекундах
     * @return true если время вышло
     */
    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
}
