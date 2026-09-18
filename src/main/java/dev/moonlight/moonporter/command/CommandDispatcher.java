package dev.moonlight.moonporter.command;

import dev.moonlight.moonporter.MoonPorter;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Регистрация команд плагина в одном месте.
 */
public final class CommandDispatcher {

    private final MoonPorter plugin;

    public CommandDispatcher(@NotNull MoonPorter plugin) {
        this.plugin = plugin;
    }

    /**
     * Регистрирует обработчики команд.
     *
     * @param command  название команды из plugin.yml
     * @param executor обработчик команды и автодополнения
     */
    public void registerCommand(@NotNull String command, @NotNull TabExecutor executor) {

        PluginCommand pluginCommand = plugin.getCommand(command);

        if (pluginCommand == null) {

            plugin.getLogger().warning("Команда '" + command + "' не найдена в plugin.yml.");
            return;

        }

        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter(executor);

    }
}
