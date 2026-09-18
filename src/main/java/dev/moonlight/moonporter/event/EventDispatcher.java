package dev.moonlight.moonporter.event;

import dev.moonlight.moonporter.MoonPorter;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Регистрация слушателей событий плагина.
 */
public class EventDispatcher {

    private final MoonPorter plugin;

    public EventDispatcher(@NotNull MoonPorter plugin) {
        this.plugin = plugin;
    }

    public void registerEvents(Listener @NotNull ... listeners) {

        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }

    }
}
