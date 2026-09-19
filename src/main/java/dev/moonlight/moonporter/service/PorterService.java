package dev.moonlight.moonporter.service;

import dev.moonlight.moonporter.config.MoonPorterConfig;
import dev.moonlight.moonporter.config.PorterTier;
import dev.moonlight.moonporter.config.type.CargoMode;
import dev.moonlight.moonporter.config.type.CancelReason;
import dev.moonlight.moonporter.config.type.TitleType;
import dev.moonlight.moonporter.porter.DeliverySession;
import dev.moonlight.moonporter.porter.DeliveryWatchdog;
import dev.moonlight.moonporter.porter.cargo.Cargo;
import dev.moonlight.moonporter.porter.cargo.FallingBlockVisual;
import dev.moonlight.moonporter.registry.CooldownRegistry;
import dev.moonlight.moonporter.registry.PorterRegistry;
import dev.moonlight.moonporter.registry.PorterTierRegistry;
import dev.moonlight.moonporter.util.RangeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Основная логика переноски груза: выдача, сдача и отмена.
 *
 * Класс ничего не знает о способе выдачи (NPC, команда, сторонний плагин) —
 * все точки входа вызывают одни и те же публичные методы.
 */
public final class PorterService {

    /** Длительность эффекта замедления: снимается при сдаче или отмене груза */
    private static final int EFFECT_DURATION_TICKS = Integer.MAX_VALUE;

    /** Минимальный интервал между титулами отказа в сдаче, миллисекунды */
    private static final long DENY_WARN_INTERVAL_MILLIS = 3000L;

    private final Map<UUID, Long> lastDenyWarnings = new ConcurrentHashMap<>();

    private final MoonPorterConfig config;
    private final PorterRegistry porterRegistry;
    private final PorterTierRegistry tierRegistry;
    private final CooldownRegistry cooldownRegistry;
    private final DeliveryWatchdog watchdog;
    private final CargoItemService cargoItemService;
    private final MessageService messageService;
    private final EconomyService economyService;
    private final RegionProvider regionProvider;
    private final CooldownService cooldownService;
    private final PermissionService permissionService;

    public PorterService(@NotNull MoonPorterConfig config,
                         @NotNull PorterRegistry porterRegistry,
                         @NotNull PorterTierRegistry tierRegistry,
                         @NotNull CooldownRegistry cooldownRegistry,
                         @NotNull DeliveryWatchdog watchdog,
                         @NotNull CargoItemService cargoItemService,
                         @NotNull MessageService messageService,
                         @NotNull EconomyService economyService,
                         @NotNull RegionProvider regionProvider,
                         @NotNull CooldownService cooldownService,
                         @NotNull PermissionService permissionService) {
        this.config = config;
        this.porterRegistry = porterRegistry;
        this.tierRegistry = tierRegistry;
        this.cooldownRegistry = cooldownRegistry;
        this.watchdog = watchdog;
        this.cargoItemService = cargoItemService;
        this.messageService = messageService;
        this.economyService = economyService;
        this.regionProvider = regionProvider;
        this.cooldownService = cooldownService;
        this.permissionService = permissionService;
    }

    /**
     * Выдаёт игроку случайный груз из зарегистрированных уровней.
     *
     * @param player игрок, кликнувший по NPC
     */
    public void pickup(@NotNull Player player) {

        PorterTier tier = tierRegistry.getRandomTier();

        if (tier == null) {
            return;
        }

        pickup(player, tier);

    }

    /**
     * Выдаёт игроку груз указанного уровня.
     *
     * @param player игрок
     * @param tier   уровень груза
     */
    public void pickup(@NotNull Player player, @NotNull PorterTier tier) {

        if (!config.isEnabled()) {
            return;
        }

        if (!config.isAllowedWorld(player.getWorld().getName())) {

            messageService.sendTitle(player, TitleType.PICKUP_WRONG_WORLD);
            return;

        }

        if (porterRegistry.isCarrying(player)) {

            messageService.sendTitle(player, TitleType.PICKUP_DENIED);
            return;

        }

        if (!permissionService.hasUse(player)) {

            messageService.sendTitle(player, TitleType.NO_PERMISSION);
            return;

        }

        UUID playerId = player.getUniqueId();

        if (isOnCooldown(playerId)) {

            messageService.sendTitle(player, TitleType.COOLDOWN, Map.of(
                    "seconds", cooldownRegistry.getRemainingSeconds(playerId)
            ));

            return;

        }

        CargoMode mode = config.getCargoMode();

        if (mode.hasInventory() && !cargoItemService.hasFreeSlot(player)) {

            messageService.sendTitle(player, TitleType.PICKUP_NO_SPACE);
            return;

        }

        Cargo cargo = new Cargo(
                config.getMaterial(),
                tier,
                messageService.applyPlaceholders(tier.name(), Map.of(
                        "player", player.getName(),
                        "tier", tier.id(),
                        "weight", tier.weight()
                )),
                player.getLocation().clone()
        );

        if (mode.hasInventory()) {
            cargoItemService.give(player, cargo);
        }

        FallingBlockVisual visual = mode.hasHead()
                ? FallingBlockVisual.spawn(player, cargo, config.isCargoNameVisible())
                : new FallingBlockVisual(null);

        attachWeightEffect(player, tier.weight());

        long expiresAt = System.currentTimeMillis() + config.getDeliveryTimeoutMillis();

        porterRegistry.start(new DeliverySession(playerId, cargo, visual, expiresAt));
        watchdog.ensureRunning();

        if (config.isCooldownEnabled() && !cooldownService.isBypassed(player)) {
            cooldownRegistry.start(playerId, config.getCooldownMillis());
        }

        messageService.sendTitle(player, TitleType.PICKUP_SUCCESS, Map.of(
                "player", player.getName(),
                "tier", tier.id(),
                "weight", tier.weight(),
                "seconds", config.getDeliveryTimeoutMillis() / 1000L
        ));

    }

    /**
     * Пытается сдать груз в текущей точке.
     *
     * @param player игрок, сдающий груз
     */
    public void deliver(@NotNull Player player) {

        if (!config.isEnabled()) {
            return;
        }

        DeliverySession session = porterRegistry.getSession(player.getUniqueId());

        if (session == null) {
            return;
        }

        if (!config.isAllowedWorld(player.getWorld().getName())) {

            denyDelivery(player, TitleType.DELIVERY_WRONG_WORLD);
            return;

        }

        if (!isAllowedDeliveryPoint(player, session.cargo())) {

            denyDelivery(player, TitleType.DELIVERY_WRONG_POINT);
            return;

        }

        if (config.getCargoMode().hasInventory() && !cargoItemService.hasCargoItem(player)) {

            cancel(player, CancelReason.CARGO_LOST);
            return;

        }

        int reward = resolveReward(session.cargo());

        economyService.deposit(player, reward);

        cancel(player, CancelReason.DELIVERED, Map.of(
                "amount", messageService.formatReward(reward)
        ));

    }

    /**
     * Отменяет переноску по причине и уведомляет игрока.
     *
     * @param player игрок
     * @param reason причина отмены
     */
    public void cancel(@NotNull Player player, @NotNull CancelReason reason) {
        cancel(player, reason, null);
    }

    /**
     * Отменяет переноску по причине с подстановкой плейсхолдеров в титул.
     *
     * @param player       игрок
     * @param reason       причина отмены
     * @param placeholders плейсхолдеры для титула
     */
    public void cancel(@NotNull Player player,
                       @NotNull CancelReason reason,
                       @Nullable Map<String, ?> placeholders) {

        DeliverySession session = porterRegistry.take(player.getUniqueId());

        if (session == null) {
            return;
        }

        lastDenyWarnings.remove(player.getUniqueId());

        session.visual().remove();

        if (config.getCargoMode().hasInventory()) {
            cargoItemService.removeAll(player);
        }

        removeWeightEffect(player);

        if (!reason.isSilent() && player.isOnline()) {
            messageService.sendTitle(player, reason.getTitleType(), placeholders);
        }

    }

    /**
     * Снимает все грузы — используется при выключении плагина и на /reload.
     * Визуализации удаляются из мира, эффекты снимаются с онлайн-игроков.
     *
     * @return количество снятых переносок
     */
    public int cancelAll() {

        List<DeliverySession> sessions = porterRegistry.snapshot();

        for (DeliverySession session : sessions) {

            session.visual().remove();

            Player player = Bukkit.getPlayer(session.playerId());

            if (player != null && player.isOnline()) {
                removeWeightEffect(player);
            }

        }

        porterRegistry.clear();
        cooldownRegistry.clear();
        lastDenyWarnings.clear();

        return sessions.size();

    }

    /**
     * Считает случайную награду за груз.
     *
     * @param cargo описание груза
     * @return сумма награды
     */
    private int resolveReward(@NotNull Cargo cargo) {

        PorterTier tier = cargo.tier();

        if (tier.rewardMax() <= tier.rewardMin()) {
            return Math.max(0, tier.rewardMin());
        }

        return RangeUtil.random(
                new int[]{tier.rewardMin(), tier.rewardMax()},
                tier.rewardMin()
        );

    }

    /**
     * Накладывает эффект замедления, соответствующий весу груза.
     *
     * @param player несущий игрок
     * @param weight вес груза из конфига
     */
    private void attachWeightEffect(@NotNull Player player, int weight) {

        if (weight <= 0) {
            return;
        }

        // ambient=false, particles=false — клиенту не отправляются лишние пакеты частиц
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW,
                EFFECT_DURATION_TICKS,
                weight - 1,
                false,
                false
        ));

    }

    /**
     * Снимает эффект замедления.
     *
     * @param player игрок
     */
    private void removeWeightEffect(@NotNull Player player) {
        player.removePotionEffect(PotionEffectType.SLOW);
    }

    /**
     * Проверяет задержку между переносками.
     *
     * @param playerId UUID игрока
     * @return true если действует кулдаун
     */
    private boolean isOnCooldown(@NotNull UUID playerId) {

        if (!config.isCooldownEnabled()) {
            return false;
        }

        return cooldownRegistry.isActive(playerId);

    }

    /**
     * Проверяет точку сдачи: радиус от места выдачи и регион WorldGuard.
     *
     * @param player игрок
     * @param cargo  описание груза
     * @return true если сдача разрешена
     */
    private boolean isAllowedDeliveryPoint(@NotNull Player player, @NotNull Cargo cargo) {

        if (!cargo.isWithinRadius(player, config.getDeliveryRadiusSquared())) {
            return false;
        }

        if (config.getAllowedRegions().isEmpty()) {
            return true;
        }

        return regionProvider.isInAnyRegion(player.getLocation(), config.getAllowedRegions());

    }

    /**
     * Отправляет титул отказа в сдаче, не чаще раза в несколько секунд:
     * при SNEAK_HOLD событие приходит на каждый переход между блоками.
     *
     * @param player игрок
     * @param type   тип титула отказа
     */
    private void denyDelivery(@NotNull Player player, @NotNull TitleType type) {

        long now = System.currentTimeMillis();
        Long last = lastDenyWarnings.get(player.getUniqueId());

        if (last != null && now - last < DENY_WARN_INTERVAL_MILLIS) {
            return;
        }

        lastDenyWarnings.put(player.getUniqueId(), now);
        messageService.sendTitle(player, type);

    }
}
