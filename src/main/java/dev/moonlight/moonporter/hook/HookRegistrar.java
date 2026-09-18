package dev.moonlight.moonporter.hook;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.service.DefaultRegionProvider;
import dev.moonlight.moonporter.service.PorterService;
import dev.moonlight.moonporter.service.RegionProvider;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Подключает необязательные интеграции.
 *
 * Классы зависимостей загружаются лениво и только после проверки
 * наличия плагина на сервере: при отсутствии Citizens или WorldGuard
 * MoonPorter продолжает работать в ограниченном режиме.
 */
public final class HookRegistrar {

    private final MoonPorter plugin;

    private @Nullable PorterService porterService;

    public HookRegistrar(@NotNull MoonPorter plugin) {
        this(plugin, null);
    }

    public HookRegistrar(@NotNull MoonPorter plugin, @Nullable PorterService porterService) {
        this.plugin = plugin;
        this.porterService = porterService;
    }

    /**
     * Подвязывает сервис переноски после его создания.
     *
     * @param porterService сервис переноски
     */
    public void bind(@NotNull PorterService porterService) {
        this.porterService = porterService;
    }

    /**
     * Регистрирует слушатель Citizens, если плагин установлен.
     *
     * @return true если интеграция подключена
     */
    public boolean registerCitizens() {

        if (porterService == null) {

            plugin.getLogger().warning("Citizens не подключён: сервис переноски не инициализирован.");
            return false;

        }

        if (!isPluginEnabled("Citizens")) {

            plugin.getLogger().warning("Citizens не найден — выдача груза через NPC отключена. "
                    + "Груз можно выдать командой /moonporter give "
                    + "или вызовом PorterService#pickup из другого плагина.");
            return false;

        }

        try {

            new CitizensHook(plugin, porterService).register();
            return true;

        } catch (Throwable throwable) {

            plugin.getLogger().warning("Не удалось подключить Citizens: " + throwable.getMessage());
            return false;

        }
    }

    /**
     * Создаёт провайдера регионов.
     * Без WorldGuard возвращает реализацию, пропускающую все проверки.
     *
     * @return провайдер регионов
     */
    public @NotNull RegionProvider resolveRegionProvider() {

        if (!isPluginEnabled("WorldGuard")) {

            plugin.getLogger().warning("WorldGuard не найден — регионы из config.yml не проверяются, "
                    + "груз можно сдать в любом месте разрешённого мира.");

            return new DefaultRegionProvider();

        }

        try {

            return new WorldGuardRegionProvider();

        } catch (Throwable throwable) {

            plugin.getLogger().warning("Не удалось подключить WorldGuard: " + throwable.getMessage());
            return new DefaultRegionProvider();

        }
    }

    /**
     * Проверяет, установлен и включён ли сторонний плагин.
     * Проверка не загружает классы зависимости.
     *
     * @param name имя плагина
     * @return true если плагин доступен
     */
    private boolean isPluginEnabled(@NotNull String name) {

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        Plugin dependency = pluginManager.getPlugin(name);

        return dependency != null && dependency.isEnabled();

    }
}
