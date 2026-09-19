package dev.moonlight.moonporter.porter;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.config.MoonPorterConfig;
import dev.moonlight.moonporter.config.type.CancelReason;
import dev.moonlight.moonporter.registry.PorterRegistry;
import dev.moonlight.moonporter.service.DeliveryBossBarService;
import dev.moonlight.moonporter.service.PorterService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Наблюдатель за активными переносками.
 *
 * Вместо отдельного BukkitRunnable на каждого игрока здесь одна задача,
 * которая существует только пока есть активные грузы и сама себя останавливает.
 * Проверки идут от дешёвых к дорогим: игрок онлайн -> истечение времени ->
 * полёт -> игровой режим.
 */
public final class DeliveryWatchdog {

    private static final long PERIOD_TICKS = 20L;

    private final MoonPorter plugin;
    private final PorterRegistry registry;
    private final MoonPorterConfig config;
    private final DeliveryBossBarService bossBarService;

    private @Nullable PorterService porterService;
    private @Nullable BukkitTask task;

    public DeliveryWatchdog(@NotNull MoonPorter plugin,
                            @NotNull PorterRegistry registry,
                            @NotNull MoonPorterConfig config,
                            @NotNull DeliveryBossBarService bossBarService) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = config;
        this.bossBarService = bossBarService;
    }

    /**
     * Связывает наблюдателя с сервисом переноски.
     * Вызывается один раз при включении плагина — до этого момента
     * задача не запускается, так как отменять сессии нечем.
     *
     * @param porterService сервис переноски
     */
    public void bind(@NotNull PorterService porterService) {
        this.porterService = porterService;
    }

    /**
     * Запускает задачу наблюдения, если она ещё не работает.
     * Вызывается при каждой выдаче груза.
     */
    public void ensureRunning() {

        if (task != null || porterService == null) {
            return;
        }

        task = new BukkitRunnable() {

            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, PERIOD_TICKS, PERIOD_TICKS);

    }

    /**
     * Полная остановка при выключении плагина.
     */
    public void shutdown() {
        stop();
    }

    /**
     * Один проход по всем активным сессиям.
     */
    private void tick() {

        if (porterService == null) {
            return;
        }

        if (registry.size() == 0) {

            stop();
            return;

        }

        long now = System.currentTimeMillis();

        for (DeliverySession session : registry.snapshot()) {

            Player player = Bukkit.getPlayer(session.playerId());

            if (player == null || !player.isOnline()) {
                continue;
            }

            bossBarService.update(player, session, now);

            CancelReason reason = detectViolation(player, session, now);

            if (reason == null) {
                continue;
            }

            applyViolationEffects(player, reason);
            porterService.cancel(player, reason);

        }
    }

    /**
     * Ищет причину отмены переноски.
     *
     * @param player  несущий игрок
     * @param session активная сессия
     * @param now     текущее время в миллисекундах
     * @return причина отмены либо null, если нарушений нет
     */
    private @Nullable CancelReason detectViolation(@NotNull Player player,
                                                   @NotNull DeliverySession session,
                                                   long now) {

        // Сущность груза могла быть уничтожена сторонним плагином:
        // такую сессию держать бессмысленно, уведомление не отправляется.
        if (!session.visual().isAlive()) {
            return CancelReason.CARGO_LOST;
        }

        if (session.isExpired(now)) {
            return CancelReason.EXPIRED;
        }

        if (player.isFlying()) {
            return CancelReason.FLYING;
        }

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return CancelReason.GAMEMODE;
        }

        return null;

    }

    /**
     * Применяет последствия нарушения, разрешённые конфигурацией.
     * Груз изымается в любом случае — здесь только сброс состояния игрока.
     *
     * @param player несущий игрок
     * @param reason причина отмены
     */
    private void applyViolationEffects(@NotNull Player player, @NotNull CancelReason reason) {

        if (reason == CancelReason.FLYING && config.isResetFlightEnabled()) {

            player.setFlying(false);
            player.setAllowFlight(false);

        }

        if (reason == CancelReason.GAMEMODE && config.isResetGamemodeEnabled()) {
            player.setGameMode(GameMode.SURVIVAL);
        }

    }

    /**
     * Останавливает задачу, если активных переносок не осталось.
     */
    private void stop() {

        if (task == null) {
            return;
        }

        task.cancel();
        task = null;

    }
}
