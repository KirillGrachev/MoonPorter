package dev.moonlight.moonporter.service;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.config.MoonPorterConfig;
import dev.moonlight.moonporter.porter.DeliverySession;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BossBar таймера доставки.
 *
 * Полоса живёт только на время активной переноски: создаётся при выдаче,
 * обновляется тиком наблюдателя и снимается при любой отмене сессии.
 * Отключается настройкой settings.delivery.bossbar.enabled —
 * тогда сервис не создаёт полос вовсе.
 */
public final class DeliveryBossBarService {

    private final MoonPorter plugin;
    private final MoonPorterConfig config;
    private final MessageService messageService;

    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> totals = new ConcurrentHashMap<>();

    public DeliveryBossBarService(@NotNull MoonPorter plugin,
                                  @NotNull MoonPorterConfig config,
                                  @NotNull MessageService messageService) {
        this.plugin = plugin;
        this.config = config;
        this.messageService = messageService;
    }

    /**
     * Показывает полосу таймера игроку.
     *
     * @param player  несущий игрок
     * @param session активная сессия
     */
    public void show(@NotNull Player player, @NotNull DeliverySession session) {

        if (!config.isBossBarEnabled()) {
            return;
        }

        hide(player);

        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();

        BossBar bar = Bukkit.createBossBar(
                new NamespacedKey(plugin, "delivery_" + playerId),
                title(player, session, now),
                config.getBossBarColor(),
                BarStyle.SOLID
        );

        bar.setProgress(1.0D);
        bar.addPlayer(player);

        bars.put(playerId, bar);
        totals.put(playerId, Math.max(1L, session.expiresAt() - now));

    }

    /**
     * Обновляет прогресс и текст полосы.
     *
     * @param player  несущий игрок
     * @param session активная сессия
     * @param now     текущее время в миллисекундах
     */
    public void update(@NotNull Player player, @NotNull DeliverySession session, long now) {

        BossBar bar = bars.get(player.getUniqueId());

        if (bar == null) {
            return;
        }

        long remaining = Math.max(0L, session.expiresAt() - now);
        long total = totals.getOrDefault(player.getUniqueId(), 1L);

        bar.setProgress(Math.min(1.0D, (double) remaining / total));
        bar.setTitle(title(player, session, now));

    }

    /**
     * Снимает полосу игрока.
     *
     * @param player игрок
     */
    public void hide(@NotNull Player player) {

        UUID playerId = player.getUniqueId();
        BossBar bar = bars.remove(playerId);

        totals.remove(playerId);

        if (bar != null) {
            bar.removeAll();
        }

    }

    /**
     * Снимает все полосы: перезагрузка, остановка плагина.
     */
    public void hideAll() {

        bars.values().forEach(BossBar::removeAll);
        bars.clear();
        totals.clear();

    }

    /**
     * Собирает текст полосы из шаблона конфига.
     *
     * @param player  несущий игрок
     * @param session активная сессия
     * @param now     текущее время в миллисекундах
     * @return окрашенный текст с плейсхолдерами
     */
    private @NotNull String title(@NotNull Player player, @NotNull DeliverySession session, long now) {

        long seconds = (Math.max(0L, session.expiresAt() - now) + 999L) / 1000L;

        return messageService.applyPlaceholders(config.getBossBarText(), Map.of(
                "seconds", seconds,
                "cargo", session.cargo().displayName(),
                "player", player.getName()
        ));

    }
}
