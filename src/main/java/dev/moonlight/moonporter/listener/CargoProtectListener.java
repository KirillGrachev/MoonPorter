package dev.moonlight.moonporter.listener;

import dev.moonlight.moonporter.config.type.TitleType;
import dev.moonlight.moonporter.service.CargoItemService;
import dev.moonlight.moonporter.service.MessageService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * Полная защита предмета груза от действий игрока.
 *
 * Груз нельзя перетащить, выбросить, положить во вторую руку,
 * поставить блоком, вставить в рамку, горшок, книжную полку или
 * компостер, собрать двойным кликом или вынести из инвентаря любым
 * кликом: каждое такое действие отменяется с титулом cargo.protected.
 * Без этих пломб предмет можно вывести из-под учёта сессии и задюпать.
 *
 * В режиме HEAD предмета не существует, и все проверки проходят
 * транзитом: isCargoItem не находит метку.
 */
public final class CargoProtectListener implements Listener {

    /** Блоки, которые принимают и расходуют предмет по правому клику */
    private static final Set<Material> CONSUMING_BLOCKS = EnumSet.of(
            Material.DECORATED_POT,
            Material.CHISELED_BOOKSHELF,
            Material.COMPOSTER
    );

    private final CargoItemService cargoItemService;
    private final MessageService messageService;

    public CargoProtectListener(@NotNull CargoItemService cargoItemService,
                                @NotNull MessageService messageService) {
        this.cargoItemService = cargoItemService;
        this.messageService = messageService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(@NotNull PlayerDropItemEvent event) {

        if (!cargoItemService.isCargoItem(event.getItemDrop().getItemStack())) {
            return;
        }

        deny(event.getPlayer());
        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(@NotNull PlayerSwapHandItemsEvent event) {

        if (!cargoItemService.isCargoItem(event.getMainHandItem())
                && !cargoItemService.isCargoItem(event.getOffHandItem())) {
            return;
        }

        deny(event.getPlayer());
        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(@NotNull InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (isCargoInvolved(event, player)) {

            deny(player);
            event.setCancelled(true);

        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(@NotNull InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        boolean cargoDragged = event.getNewItems().values().stream()
                .anyMatch(cargoItemService::isCargoItem);

        if (!cargoDragged) {
            return;
        }

        deny(player);
        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreativeClick(@NotNull InventoryCreativeEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!cargoItemService.isCargoItem(event.getCurrentItem())
                && !cargoItemService.isCargoItem(event.getCursor())) {
            return;
        }

        deny(player);
        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {

        if (!cargoItemService.isCargoItem(event.getItemInHand())) {
            return;
        }

        deny(event.getPlayer());
        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(@NotNull HangingPlaceEvent event) {

        if (!cargoItemService.isCargoItem(event.getItemStack())) {
            return;
        }

        if (event.getPlayer() != null) {
            deny(event.getPlayer());
        }

        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(@NotNull PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();

        if (!cargoItemService.isCargoItem(handItem(player, event.getHand()))) {
            return;
        }

        deny(player);
        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null || !CONSUMING_BLOCKS.contains(block.getType())) {
            return;
        }

        Player player = event.getPlayer();

        if (!cargoItemService.isCargoItem(handItem(player, event.getHand()))) {
            return;
        }

        deny(player);
        event.setCancelled(true);

    }

    /**
     * Участвует ли груз в клике: текущий слот, курсор, офхенд-своп
     * или hotbar-слот, выбранный клавишей номера.
     *
     * @param event  событие клика
     * @param player игрок
     * @return true если клик нужно отменить
     */
    private boolean isCargoInvolved(@NotNull InventoryClickEvent event, @NotNull Player player) {

        if (cargoItemService.isCargoItem(event.getCurrentItem())
                || cargoItemService.isCargoItem(event.getCursor())) {
            return true;
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND
                && cargoItemService.isCargoItem(player.getInventory().getItemInOffHand())) {
            return true;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {

            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            return cargoItemService.isCargoItem(hotbarItem);

        }

        // Двойной клик собирает stacks того же материала и утащил бы груз в курсор
        return event.getClick() == ClickType.DOUBLE_CLICK
                && cargoItemService.hasCargoItem(player);

    }

    /**
     * Предмет в руке, которой совершено действие.
     *
     * @param player игрок
     * @param hand   рука из события
     * @return предмет руки либо null
     */
    private @Nullable ItemStack handItem(@NotNull Player player, @NotNull EquipmentSlot hand) {

        return hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

    }

    /**
     * Отменяемое действие: единый титул пломбы.
     *
     * @param player игрок
     */
    private void deny(@NotNull Player player) {
        messageService.sendTitle(player, TitleType.CARGO_PROTECTED);
    }
}
