package dev.moonlight.moonporter.registry;

import dev.moonlight.moonporter.porter.DeliverySession;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр активных переносок.
 *
 * Ключ — UUID, а не объект Player: реестр не удерживает сильные ссылки
 * на игроков и не течёт при нештатном завершении сессии.
 * ConcurrentHashMap вместо HashMap — чтение происходит из таска таймера,
 * запись из потока событий.
 */
public final class PorterRegistry {

    private final Map<UUID, DeliverySession> sessions = new ConcurrentHashMap<>();

    /**
     * Регистрирует новую сессию переноски.
     *
     * @param session сессия груза
     */
    public void start(@NotNull DeliverySession session) {
        sessions.put(session.playerId(), session);
    }

    /**
     * Возвращает активную сессию игрока.
     *
     * @param playerId UUID игрока
     * @return сессия либо null
     */
    public @Nullable DeliverySession getSession(@NotNull UUID playerId) {
        return sessions.get(playerId);
    }

    /**
     * Возвращает активную сессию игрока и удаляет её из реестра.
     *
     * @param playerId UUID игрока
     * @return сессия либо null, если активной переноски нет
     */
    public @Nullable DeliverySession take(@NotNull UUID playerId) {
        return sessions.remove(playerId);
    }

    /**
     * Проверяет наличие активной переноски.
     *
     * @param player игрок
     * @return true если игрок уже несёт груз
     */
    public boolean isCarrying(@NotNull Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /**
     * Снимок всех активных сессий для безопасного обхода.
     *
     * @return неизменяемый список сессий
     */
    public @NotNull List<DeliverySession> snapshot() {
        return List.copyOf(sessions.values());
    }

    /**
     * Количество активных переносок.
     *
     * @return число сессий
     */
    public int size() {
        return sessions.size();
    }

    public void clear() {
        sessions.clear();
    }
}
