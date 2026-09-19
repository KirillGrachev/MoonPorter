package dev.moonlight.moonporter.porter.cargo;

import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Груз над головой: падающий блок верхом на игроке.
 *
 * Все настройки подобраны так, чтобы сущность не создавала нагрузки:
 * нет гравитации, нет физики падения, нет урона, нет частиц,
 * нет сохранения в регион-файлы мира.
 *
 * Режим Marker у FallingBlock не существует ни в ванильном NBT,
 * ни в Bukkit API (тег Marker есть только у ArmorStand и сущности
 * minecraft:marker), поэтому здесь он не применяется: падающий блок
 * и без того не сталкивается с игроками и не нажимает плиты.
 */
public final class FallingBlockVisual {

    private final @Nullable FallingBlock fallingBlock;

    public FallingBlockVisual(@Nullable FallingBlock fallingBlock) {
        this.fallingBlock = fallingBlock;
    }

    /**
     * Спавнит визуализацию груза и прикрепляет её к игроку.
     *
     * @param player      несущий игрок
     * @param cargo       описание груза
     * @param nameVisible показывать ли имя груза над блоком
     * @return прикреплённая визуализация
     */
    public static @NotNull FallingBlockVisual spawn(@NotNull Player player,
                                                    @NotNull Cargo cargo,
                                                    boolean nameVisible) {

        FallingBlock fallingBlock = player.getWorld().spawnFallingBlock(
                player.getLocation(),
                cargo.material().createBlockData()
        );

        FallingBlockVisual visual = new FallingBlockVisual(fallingBlock);

        visual.applyName(cargo.displayName(), nameVisible);
        visual.attach(player);

        return visual;

    }

    /**
     * Прикрепляет визуализацию к игроку.
     *
     * @param player несущий игрок
     */
    public void attach(@NotNull Player player) {

        if (fallingBlock == null) {
            return;
        }

        fallingBlock.setVelocity(new Vector(0, 0, 0));
        fallingBlock.setFallDistance(0.0F);
        fallingBlock.setDropItem(false);
        fallingBlock.setGravity(false);
        fallingBlock.setHurtEntities(false);
        fallingBlock.setInvulnerable(true);

        // Сущность не должна попадать в сохранения мира:
        // иначе брошенный груз остаётся в чанке навсегда.
        fallingBlock.setPersistent(false);

        player.addPassenger(fallingBlock);

    }

    /**
     * Применяет имя груза к визуализации.
     *
     * @param name    окрашенное имя с подставленными плейсхолдерами
     * @param visible показывать ли неймтейг
     */
    public void applyName(@NotNull String name, boolean visible) {

        if (fallingBlock == null) {
            return;
        }

        fallingBlock.setCustomName(name);
        fallingBlock.setCustomNameVisible(visible);

    }

    /**
     * Удаляет визуализацию из мира.
     * Вызов идемпотентен.
     */
    public void remove() {

        if (fallingBlock == null || !fallingBlock.isValid()) {
            return;
        }

        fallingBlock.remove();

    }

    /**
     * В порядке ли визуализация.
     *
     * @return true если визуал отключён (сущности нет и терять нечего)
     *         или сущность жива; false если сущность уничтожили извне
     */
    public boolean isAlive() {
        return fallingBlock == null || fallingBlock.isValid();
    }
}
