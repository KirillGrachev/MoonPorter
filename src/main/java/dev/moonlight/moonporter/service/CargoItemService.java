package dev.moonlight.moonporter.service;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.porter.cargo.Cargo;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Представление груза в инвентаре игрока.
 *
 * Груз лежит в инвентаре обычным предметом с именем уровня и меткой
 * в PersistentDataContainer: метка переживает переименование и
 * однозначно отличает груз от любого другого предмета того же материала.
 */
public final class CargoItemService {

    private final NamespacedKey cargoKey;
    private final NamespacedKey cargoIdKey;

    public CargoItemService(@NotNull MoonPorter plugin) {
        this.cargoKey = new NamespacedKey(plugin, "cargo");
        this.cargoIdKey = new NamespacedKey(plugin, "cargo_id");
    }

    /**
     * Создаёт предмет груза.
     *
     * @param cargo описание груза
     * @return предмет с именем уровня и меткой груза
     */
    public @NotNull ItemStack create(@NotNull Cargo cargo) {

        ItemStack item = new ItemStack(cargo.material());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(cargo.displayName());
            meta.getPersistentDataContainer().set(cargoKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(cargoIdKey, PersistentDataType.STRING, cargo.id().toString());
            item.setItemMeta(meta);

        }

        return item;

    }

    /**
     * Проверяет, является ли предмет грузом.
     *
     * @param item проверяемый предмет
     * @return true если предмет помечен как груз
     */
    public boolean isCargoItem(@Nullable ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        return meta != null
                && meta.getPersistentDataContainer().has(cargoKey, PersistentDataType.BYTE);

    }

    /**
     * Читает уникальный идентификатор груза из предмета.
     *
     * @param item проверяемый предмет
     * @return id груза либо null, если предмет не груз
     */
    public @Nullable String getCargoId(@Nullable ItemStack item) {

        if (!isCargoItem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        return meta == null
                ? null
                : meta.getPersistentDataContainer().get(cargoIdKey, PersistentDataType.STRING);

    }

    /**
     * Проверяет, лежит ли груз с конкретным id в инвентаре игрока.
     *
     * @param player проверяемый игрок
     * @param cargoId идентификатор экземпляра груза
     * @return true если найден именно этот груз
     */
    public boolean hasCargoItem(@NotNull Player player, @NotNull String cargoId) {

        return Arrays.stream(player.getInventory().getStorageContents())
                .anyMatch(stack -> cargoId.equals(getCargoId(stack)));

    }

    /**
     * Проверяет, лежит ли какой-либо груз в инвентаре игрока.
     *
     * @param player проверяемый игрок
     * @return true если предмет груза найден
     */
    public boolean hasCargoItem(@NotNull Player player) {

        return Arrays.stream(player.getInventory().getStorageContents())
                .anyMatch(this::isCargoItem);

    }

    /**
     * Убирает из инвентаря груз с конкретным id.
     *
     * @param player  игрок
     * @param cargoId идентификатор экземпляра груза
     */
    public void removeById(@NotNull Player player, @NotNull String cargoId) {

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();

        for (int slot = 0; slot < contents.length; slot++) {

            if (cargoId.equals(getCargoId(contents[slot]))) {
                inventory.setItem(slot, null);
            }

        }
    }

    /**
     * Проверяет наличие свободного слота под груз.
     *
     * @param player проверяемый игрок
     * @return true если груз удастся положить
     */
    public boolean hasFreeSlot(@NotNull Player player) {
        return player.getInventory().firstEmpty() != -1;
    }

    /**
     * Кладёт груз в инвентарь. Вызывается после проверки свободного слота.
     *
     * @param player получатель
     * @param cargo  описание груза
     */
    public void give(@NotNull Player player, @NotNull Cargo cargo) {
        player.getInventory().addItem(create(cargo));
    }

    /**
     * Убирает все предметы груза из инвентаря.
     *
     * @param player игрок
     */
    public void removeAll(@NotNull Player player) {

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();

        for (int slot = 0; slot < contents.length; slot++) {

            if (isCargoItem(contents[slot])) {
                inventory.setItem(slot, null);
            }

        }
    }
}
