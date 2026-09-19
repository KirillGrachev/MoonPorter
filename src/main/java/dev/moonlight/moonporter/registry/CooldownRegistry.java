package dev.moonlight.moonporter.registry;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр задержек между переносками.
 * Хранит только момент окончания в миллисекундах — без задач планировщика.
 */
public final class CooldownRegistry {

    private final Map<UUID, Long> expirations = new ConcurrentHashMap<>();

    /**
     * Назначает игроку задержку.
     *
     * @param playerId UUID игрока
     * @param millis   длительность в миллисекундах, уже из конфигурации
     */
    public void start(@NotNull UUID playerId, long millis) {

        if (millis <= 0L) {
            return;
        }

        expirations.put(playerId, System.currentTimeMillis() + millis);

    }

    /**
     * Проверяет активную задержку и попутно вычищает протухшие записи.
     *
     * @param playerId UUID игрока
     * @return true если задержка ещё действует
     */
    public boolean isActive(@NotNull UUID playerId) {

        Long expiresAt = expirations.get(playerId);

        if (expiresAt == null) {
            return false;
        }

        if (expiresAt <= System.currentTimeMillis()) {

            expirations.remove(playerId, expiresAt);
            return false;

        }

        return true;

    }

    /**
     * Оставшееся время задержки в секундах.
     *
     * @param playerId UUID игрока
     * @return секунды до окончания, 0 если задержки нет
     */
    public long getRemainingSeconds(@NotNull UUID playerId) {

        Long expiresAt = expirations.get(playerId);

        if (expiresAt == null) {
            return 0L;
        }

        long remaining = expiresAt - System.currentTimeMillis();

        return remaining <= 0L ? 0L : (long) Math.ceil(remaining / 1000.0D);

    }

    public void remove(@NotNull UUID playerId) {
        expirations.remove(playerId);
    }

    public void clear() {
        expirations.clear();
    }
}
