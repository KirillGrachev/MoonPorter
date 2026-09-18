package dev.moonlight.moonporter.porter.cargo;

import dev.moonlight.moonporter.config.MoonPorterConfig;
import dev.moonlight.moonporter.config.PorterTier;
import dev.moonlight.moonporter.service.MessageService;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Создаёт груз и его визуализацию.
 *
 * Здесь же находится единственное место, где спавнится сущность, —
 * при переходе на пакетную визуализацию меняется только этот класс.
 */
public final class CargoVisualFactory {

    private final MoonPorterConfig config;
    private final MessageService messageService;

    public CargoVisualFactory(@NotNull MoonPorterConfig config,
                              @NotNull MessageService messageService) {
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

        FallingBlock fallingBlock = player.getWorld().spawnFallingBlock(
                player.getLocation(),
                cargo.material().createBlockData()
        );

        fallingBlock.setCustomName(cargo.displayName());

        FallingBlockVisual visual = new FallingBlockVisual(fallingBlock);

        visual.attach(player);

        return visual;

    }
}
