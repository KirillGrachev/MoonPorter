package dev.moonlight.moonporter.porter.cargo;

import dev.moonlight.moonporter.MoonPorter;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Груз в двух руках: BlockDisplay без физики, стоящий по центру перед
 * грудью игрока, как будто игрок обхватил его руками.
 *
 * Позиция строится от корпуса, а не от взгляда: горизонтальное смещение
 * считается по yaw тела, высота фиксирована над ногами, поэтому груз
 * не взлетает и не падает при поворотах головы и не уводится в бок.
 * Точка удерживается в settings.cargo.hands_offset.
 *
 * Позиция обновляется каждый тик телепортом без интерполяции:
 * сглаживание дисплея давало видимый лаг на поворотах.
 * Ссылка на носителя хранится напрямую: груз живёт меньше сессии,
 * а поиск игрока по UUID каждый тик был лишней работой для карты.
 * Таск живёт только пока жив груз. Требует ядро 1.19.4+:
 * на старых ядрах фабрика вернёт FallingBlockVisual.
 */
public final class BlockDisplayVisual implements CargoVisual {

    private static final long PERIOD_TICKS = 1L;

    private final MoonPorter plugin;
    private final BlockDisplay display;
    private final Player carrier;
    private final double forward;
    private final double height;
    private final double anchor;

    private @Nullable BukkitTask task;

    public BlockDisplayVisual(@NotNull MoonPorter plugin,
                              @NotNull Player player,
                              @NotNull Cargo cargo,
                              double forward,
                              double height,
                              double anchor) {

        this.plugin = plugin;
        this.carrier = player;
        this.forward = forward;
        this.height = height;
        this.anchor = anchor;

        this.display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);

        this.display.setBlock(cargo.material().createBlockData());
        this.display.setPersistent(false);
        this.display.setInvulnerable(true);

    }

    @Override
    public void attach(@NotNull Player player) {

        follow();

        this.task = new BukkitRunnable() {

            @Override
            public void run() {
                follow();
            }
        }.runTaskTimer(plugin, PERIOD_TICKS, PERIOD_TICKS);

    }

    @Override
    public void remove() {

        if (task != null) {

            task.cancel();
            task = null;

        }

        if (display.isValid()) {
            display.remove();
        }

    }

    @Override
    public boolean isAlive() {
        return display.isValid();
    }

    @Override
    public void applyName(@NotNull String name, boolean visible) {

        display.setCustomName(name);
        display.setCustomNameVisible(visible);

    }

    /**
     * Ставит груз по центру перед корпусом носителя.
     * Горизонталь — по yaw тела, высота — фиксировано над ногами:
     * модель обхвата двумя руками, независимая от направления взгляда.
     *
     * Знаки компонентов соответствуют конвенции Bukkit:
     * forward = (-sin(yaw), +cos(yaw)), yaw 0 смотрит в +Z.
     *
     * Ядро рендерит блок дисплея от угла, а не от центра сущности,
     * поэтому из точки удержания вычитается поправка anchor
     * (settings.cargo.hands_offset.anchor) по каждой мировой оси.
     */
    private void follow() {

        if (!carrier.isOnline()) {
            return;
        }

        Location hands = carrier.getLocation();
        double yaw = Math.toRadians(hands.getYaw());

        hands.setX(hands.getX() - Math.sin(yaw) * forward - anchor);
        hands.setZ(hands.getZ() + Math.cos(yaw) * forward - anchor);
        hands.setY(hands.getY() + height - anchor);

        display.teleport(hands);

    }
}
