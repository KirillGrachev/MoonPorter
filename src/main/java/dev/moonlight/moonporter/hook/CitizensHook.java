package dev.moonlight.moonporter.hook;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.service.PorterService;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Слушатель Citizens. Выдаёт груз при клике по разрешённому NPC.
 *
 * Класс загружается только если Citizens установлен —
 * см. {@link HookRegistrar}.
 */
public final class CitizensHook implements Listener {

    private final MoonPorter plugin;
    private final PorterService porterService;

    public CitizensHook(@NotNull MoonPorter plugin, @NotNull PorterService porterService) {
        this.plugin = plugin;
        this.porterService = porterService;
    }

    /**
     * Регистрирует слушателя в PluginManager.
     */
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNpcRightClick(@NotNull NPCRightClickEvent event) {

        int npcId = event.getNPC().getId();

        if (!plugin.getConfigManager().getNpcIds().contains(npcId)) {
            return;
        }

        Player clicker = event.getClicker();

        if (clicker == null) {
            return;
        }

        porterService.pickup(clicker);

    }
}
