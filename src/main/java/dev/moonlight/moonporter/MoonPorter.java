package dev.moonlight.moonporter;

import dev.moonlight.moonporter.command.CommandDispatcher;
import dev.moonlight.moonporter.command.MoonPorterCommand;
import dev.moonlight.moonporter.config.ConfigManager;
import dev.moonlight.moonporter.config.type.CancelReason;
import dev.moonlight.moonporter.event.EventDispatcher;
import dev.moonlight.moonporter.hook.HookRegistrar;
import dev.moonlight.moonporter.listener.PorterStateListener;
import dev.moonlight.moonporter.porter.DeliveryWatchdog;
import dev.moonlight.moonporter.porter.cargo.CargoVisualFactory;
import dev.moonlight.moonporter.registry.CooldownRegistry;
import dev.moonlight.moonporter.registry.PorterRegistry;
import dev.moonlight.moonporter.registry.PorterTierRegistry;
import dev.moonlight.moonporter.service.CooldownService;
import dev.moonlight.moonporter.service.EconomyService;
import dev.moonlight.moonporter.service.MessageService;
import dev.moonlight.moonporter.service.PorterService;
import dev.moonlight.moonporter.service.RegionProvider;
import dev.moonlight.moonporter.service.ReloadService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
        EconomyService economyService = new EconomyService(resolveEconomy());

        PorterRegistry porterRegistry = new PorterRegistry();
        PorterTierRegistry tierRegistry = new PorterTierRegistry(configManager);
        CooldownRegistry cooldownRegistry = new CooldownRegistry();
        CooldownService cooldownService = new CooldownService(configManager);
        CargoVisualFactory cargoVisualFactory = new CargoVisualFactory(configManager, messageService);

        // Watchdog и HookRegistrar создаются до PorterService,
        // потому что сами нужны ему в конструкторе. Обратная ссылка
        // проставляется через bind() сразу после создания сервиса.
        this.deliveryWatchdog = new DeliveryWatchdog(this, porterRegistry);

        HookRegistrar hookRegistrar = new HookRegistrar(this);
        RegionProvider regionProvider = hookRegistrar.resolveRegionProvider();

        this.porterService = new PorterService(
                configManager,
                porterRegistry,
                tierRegistry,
                cooldownRegistry,
                deliveryWatchdog,
                cargoVisualFactory,
                messageService,
                economyService,
                regionProvider,
                cooldownService
        );

        ReloadService reloadService = new ReloadService(configManager, tierRegistry, porterService, messageService);

        hookRegistrar.bind(porterService);
        deliveryWatchdog.bind(porterService);

        // Регистрация слушателей
        new EventDispatcher(this).registerEvents(
                new PorterStateListener(configManager, porterService)
        );

        // Регистрация команд
        new CommandDispatcher(this).registerCommand(
                "moonporter",
                new MoonPorterCommand(configManager, tierRegistry, messageService, porterService, reloadService)
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

        // Итерация по онлайн-игрокам вместо Bukkit.getOfflinePlayers():
        // снимок не содержит тысяч записей и не вызывает return из цикла.
        for (Player player : Bukkit.getOnlinePlayers()) {
            porterService.cancel(player, CancelReason.SHUTDOWN);
        }

        porterService.cancelAll();

        deliveryWatchdog.shutdown();

        getServer().getScheduler().cancelTasks(this);

        getLogger().info("MoonPorter остановлен!");

    }

    /**
     * Ищет провайдер экономики Vault.
     * Отсутствие Vault не является ошибкой — плагин продолжает работать.
     *
     * @return провайдер экономики либо null
     */
    private Economy resolveEconomy() {

        RegisteredServiceProvider<Economy> registration =
                getServer().getServicesManager().getRegistration(Economy.class);

        return registration == null ? null : registration.getProvider();

    }

    public @NotNull ConfigManager getConfigManager() {
        return configManager;
    }
}
