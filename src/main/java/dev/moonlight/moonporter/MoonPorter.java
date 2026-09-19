package dev.moonlight.moonporter;

import dev.moonlight.moonporter.command.CommandDispatcher;
import dev.moonlight.moonporter.command.MoonPorterCommand;
import dev.moonlight.moonporter.config.ConfigManager;
import dev.moonlight.moonporter.config.type.CancelReason;
import dev.moonlight.moonporter.event.EventDispatcher;
import dev.moonlight.moonporter.hook.HookRegistrar;
import dev.moonlight.moonporter.hook.VaultEconomyHook;
import dev.moonlight.moonporter.listener.CargoProtectListener;
import dev.moonlight.moonporter.listener.PorterStateListener;
import dev.moonlight.moonporter.porter.DeliveryWatchdog;
import dev.moonlight.moonporter.registry.CooldownRegistry;
import dev.moonlight.moonporter.registry.PorterRegistry;
import dev.moonlight.moonporter.registry.PorterTierRegistry;
import dev.moonlight.moonporter.service.CargoItemService;
import dev.moonlight.moonporter.service.CooldownService;
import dev.moonlight.moonporter.service.DeliveryBossBarService;
import dev.moonlight.moonporter.service.EconomyService;
import dev.moonlight.moonporter.service.MessageService;
import dev.moonlight.moonporter.service.PermissionService;
import dev.moonlight.moonporter.service.PorterService;
import dev.moonlight.moonporter.service.RegionProvider;
import dev.moonlight.moonporter.service.ReloadService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Точка входа плагина.
 *
 * Все зависимости собираются здесь и передаются в конструкторы —
 * статические обращения к getInstance() из бизнес-логики исключены.
 * Полями остаются только те объекты, которые нужны после onEnable.
 */
public final class MoonPorter extends JavaPlugin {

    private ConfigManager configManager;
    private DeliveryWatchdog deliveryWatchdog;
    private PorterService porterService;

    @Override
    public void onEnable() {

        this.configManager = new ConfigManager(this);

        MessageService messageService = new MessageService(configManager);
        EconomyService economyService = new EconomyService(attachEconomy());

        PorterRegistry porterRegistry = new PorterRegistry();
        PorterTierRegistry tierRegistry = new PorterTierRegistry(configManager);
        CooldownRegistry cooldownRegistry = new CooldownRegistry();
        PermissionService permissionService = new PermissionService(configManager);
        CooldownService cooldownService = new CooldownService(configManager, permissionService);
        CargoItemService cargoItemService = new CargoItemService(this);
        DeliveryBossBarService bossBarService = new DeliveryBossBarService(this, configManager, messageService);

        // Watchdog и HookRegistrar создаются до PorterService,
        // потому что сами нужны ему в конструкторе. Обратная ссылка
        // проставляется через bind() сразу после создания сервиса.
        this.deliveryWatchdog = new DeliveryWatchdog(this, porterRegistry, configManager, bossBarService);

        HookRegistrar hookRegistrar = new HookRegistrar(this);
        RegionProvider regionProvider = hookRegistrar.resolveRegionProvider();

        this.porterService = new PorterService(
                configManager,
                porterRegistry,
                tierRegistry,
                cooldownRegistry,
                deliveryWatchdog,
                cargoItemService,
                messageService,
                economyService,
                regionProvider,
                cooldownService,
                permissionService,
                bossBarService
        );

        ReloadService reloadService = new ReloadService(configManager, tierRegistry, porterService, messageService);

        hookRegistrar.bind(porterService);
        deliveryWatchdog.bind(porterService);

        // Регистрация слушателей
        new EventDispatcher(this).registerEvents(
                new PorterStateListener(configManager, porterService, cargoItemService),
                new CargoProtectListener(cargoItemService, messageService)
        );

        // Регистрация команд
        new CommandDispatcher(this).registerCommand(
                "moonporter",
                new MoonPorterCommand(configManager, tierRegistry, messageService,
                        porterService, reloadService, permissionService)
        );

        hookRegistrar.registerCitizens();

        if (!economyService.isAvailable()) {

            getLogger().warning("Vault или плагин экономики не найдены — "
                    + "награда за груз выдаваться не будет.");

        }

        getLogger().info("Регионы: " + regionProvider.getName()
                + ", NPC: " + configManager.getNpcIds());

        getLogger().info("MoonPorter успешно запущен!");

    }

    @Override
    public void onDisable() {

        // onEnable мог упасть до создания сервисов: onDisable обязан быть null-safe,
        // иначе ошибка включения превращается во вторую ошибку при выключении.
        if (porterService != null) {

            // Итерация по онлайн-игрокам вместо Bukkit.getOfflinePlayers():
            // снимок не содержит тысяч записей и не вызывает return из цикла.
            for (Player player : Bukkit.getOnlinePlayers()) {
                porterService.cancel(player, CancelReason.SHUTDOWN);
            }

            porterService.cancelAll();

        }

        if (deliveryWatchdog != null) {
            deliveryWatchdog.shutdown();
        }

        getServer().getScheduler().cancelTasks(this);

        getLogger().info("MoonPorter остановлен!");

    }

    /**
     * Подключает экономику Vault, если плагин установлен.
     *
     * Проверка наличия выполняется ДО обращения к классу интеграции:
     * без этого отсутствующий в classpath Vault дал бы
     * NoClassDefFoundError на первом же вызове.
     *
     * @return хук экономики либо null, если Vault или провайдер отсутствуют
     */
    private @Nullable VaultEconomyHook attachEconomy() {

        Plugin vault = getServer().getPluginManager().getPlugin("Vault");

        if (vault == null || !vault.isEnabled()) {
            return null;
        }

        try {

            return VaultEconomyHook.attach(this);

        } catch (Throwable throwable) {

            getLogger().warning("Не удалось подключить Vault: " + throwable.getMessage());
            return null;

        }
    }

    public @NotNull ConfigManager getConfigManager() {
        return configManager;
    }
}
