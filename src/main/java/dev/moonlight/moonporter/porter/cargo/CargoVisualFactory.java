package dev.moonlight.moonporter.porter.cargo;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.config.MoonPorterConfig;
import dev.moonlight.moonporter.config.PorterTier;
import dev.moonlight.moonporter.config.type.CargoVisualType;
import dev.moonlight.moonporter.service.MessageService;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Создаёт груз и его визуализацию.
 *
 * Здесь же находится единственное место, где спавнятся сущности, —
 * при переходе на пакетную визуализацию меняется только этот класс.
 * Тип визуализации выбирается в settings.cargo.visual: HANDS требует
 * ядро 1.19.4+, на старых ядрах автоматически применяется HEAD.
 */
public final class CargoVisualFactory {

    private final MoonPorter plugin;
    private final MoonPorterConfig config;
    private final MessageService messageService;

    public CargoVisualFactory(@NotNull MoonPorter plugin,
                              @NotNull MoonPorterConfig config,
                              @NotNull MessageService messageService) {
        this.plugin = plugin;
        this.config = config;
        this.messageService = messageService;
    }

    /**
     * Создаёт груз для игрока и выбранного уровня.
     *
     * @param player игрок, получающий груз
     * @param tier   уровень груза
     * @return собранный груз
     */
    public @NotNull Cargo create(@NotNull Player player, @NotNull PorterTier tier) {

        Material material = config.getMaterial();

        String displayName = messageService.applyPlaceholders(tier.name(), Map.of(
                "player", player.getName(),
                "tier", tier.id(),
                "weight", tier.weight()
        ));

        return new Cargo(
                material,
                tier,
                displayName,
                player.getLocation().clone()
        );

    }

    /**
     * Спавнит визуализацию груза и прикрепляет её к игроку.
     *
     * @param player несущий игрок
     * @param cargo  описание груза
     * @return визуализация, прикреплённая к игроку
     */
    public @NotNull CargoVisual spawn(@NotNull Player player, @NotNull Cargo cargo) {

        if (config.getCargoVisualType() == CargoVisualType.HANDS) {

            try {

                BlockDisplayVisual visual = new BlockDisplayVisual(
                        plugin,
                        player,
                        cargo,
                        config.getHandsForward(),
                        config.getHandsHeight()
                );

                visual.attach(player);

                return visual;

            } catch (Throwable throwable) {

                plugin.getLogger().warning("Визуализация HANDS недоступна на этом ядре — "
                        + "груз будет отображаться над головой.");

            }
        }

        FallingBlock fallingBlock = player.getWorld().spawnFallingBlock(
                player.getLocation(),
                cargo.material().createBlockData()
        );

        fallingBlock.setCustomName(cargo.displayName());

        FallingBlockVisual visual = new FallingBlockVisual(fallingBlock, config.isCargoNameVisible());

        visual.attach(player);

        return visual;

    }
}
