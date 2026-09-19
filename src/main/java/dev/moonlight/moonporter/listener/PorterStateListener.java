package dev.moonlight.moonporter.listener;

import dev.moonlight.moonporter.config.MoonPorterConfig;
import dev.moonlight.moonporter.config.type.CancelReason;
import dev.moonlight.moonporter.config.type.DeliveryTrigger;
import dev.moonlight.moonporter.config.type.TitleType;
import dev.moonlight.moonporter.service.CargoItemService;
import dev.moonlight.moonporter.service.MessageService;
import dev.moonlight.moonporter.service.PorterService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Слушатель состояний игрока: сдача груза, выход с сервера, смерть.
 *
 * Сдача поддерживает два триггера из конфига:
 * SNEAK_TOGGLE — по нажатию Shift, SNEAK_HOLD — пока Shift удерживается
 * (проверка при переходе между блоками, чтобы не опрашивать регион на каждый тик).
 */
public final class PorterStateListener implements Listener {

    private final MoonPorterConfig config;
    private final PorterService porterService;
    private final MessageService messageService;
    private final CargoItemService cargoItemService;

    public PorterStateListener(@NotNull MoonPorterConfig config,
                               @NotNull PorterService porterService,
                               @NotNull MessageService messageService,
                               @NotNull CargoItemService cargoItemService) {
        this.config = config;
        this.porterService = porterService;
        this.messageService = messageService;
        this.cargoItemService = cargoItemService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleSneak(@NotNull PlayerToggleSneakEvent event) {

        if (config.getDeliveryTrigger() != DeliveryTrigger.SNEAK_TOGGLE) {
            return;
        }

        // Отпускание Shift не должно засчитываться как сдача груза
        if (!event.isSneaking()) {
            return;
        }

        porterService.deliver(event.getPlayer());

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {

        if (config.getDeliveryTrigger() != DeliveryTrigger.SNEAK_HOLD) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.isSneaking()) {
            return;
        }

        if (!isBlockChanged(event)) {
            return;
        }

        porterService.deliver(player);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        porterService.cancel(event.getPlayer(), CancelReason.QUIT);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(@NotNull PlayerDeathEvent event) {

        porterService.cancel(event.getEntity(), CancelReason.DEATH);

        // Предмет груза не должен оставаться в луте: сессия уже закрыта
        event.getDrops().removeIf(cargoItemService::isCargoItem);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(@NotNull PlayerDropItemEvent event) {

        if (!cargoItemService.isCargoItem(event.getItemDrop().getItemStack())) {
            return;
        }

        event.setCancelled(true);
        messageService.sendTitle(event.getPlayer(), TitleType.DROP_DENIED);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {

        if (event.getClick() != ClickType.DROP && event.getClick() != ClickType.CONTROL_DROP) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!cargoItemService.isCargoItem(event.getCurrentItem())) {
            return;
        }

        event.setCancelled(true);
        messageService.sendTitle(player, TitleType.DROP_DENIED);

    }

    /**
     * Проверяет, перешёл ли игрок в новый блок.
     * Без этой проверки регион опрашивался бы на каждое микродвижение.
     *
     * @param event событие перемещения
     * @return true если координаты блока изменились
     */
    private boolean isBlockChanged(@NotNull PlayerMoveEvent event) {

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return false;
        }

        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();

    }
}
