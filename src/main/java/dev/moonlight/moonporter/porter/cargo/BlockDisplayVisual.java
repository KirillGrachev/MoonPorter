package dev.moonlight.moonporter.porter.cargo;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.config.MoonPorterConfig;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Груз в двух руках: BlockDisplay без физики перед корпусом игрока
 * и невидимый маркер-armor stand для неймтейга над грузом.
 *
 * Смещение считается в локальной системе координат игрока
 * (вперёд / высота / влево), поэтому на любых четырёх сторонах света
 * груз держится одинаково относительно тела. Значения читаются из
 * конфигурации каждый тик: правка конфига и /mporter reload
 * применяются к уже несомому грузу без перевыдачи.
 *
 * Неймтейг ведёт отдельный маркер: у BlockDisplay имя прилипает
 * к центру блока, а маркер ставится над грузом на name_height.
 * Позиция обновляется каждый тик телепортом без интерполяции.
 * Таск живёт только пока жив груз. Требует ядро 1.19.4+:
 * на старых ядрах фабрика вернёт FallingBlockVisual.
 */
public final class BlockDisplayVisual implements CargoVisual {

    private static final long PERIOD_TICKS = 1L;

    private final MoonPorter plugin;
    private final MoonPorterConfig config;
    private final BlockDisplay display;
    private final ArmorStand nameTag;
    private final Player carrier;

    private @Nullable BukkitTask task;

    public BlockDisplayVisual(@NotNull MoonPorter plugin,
                              @NotNull MoonPorterConfig config,
                              @NotNull Player player,
                              @NotNull Cargo cargo) {

        this.plugin = plugin;
        this.config = config;
        this.carrier = player;

        this.display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);

        this.display.setBlock(cargo.material().createBlockData());
        this.display.setPersistent(false);
        this.display.setInvulnerable(true);

        this.nameTag = player.getWorld().spawn(player.getLocation(), ArmorStand.class);

        this.nameTag.setMarker(true);
        this.nameTag.setInvisible(true);
        this.nameTag.setGravity(false);
        this.nameTag.setPersistent(false);
        this.nameTag.setInvulnerable(true);

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

        if (nameTag.isValid()) {
            nameTag.remove();
        }

    }

    @Override
    public boolean isAlive() {
        return display.isValid();
    }

    @Override
    public void applyName(@NotNull String name, boolean visible) {

        nameTag.setCustomName(name);
        nameTag.setCustomNameVisible(visible);

    }

    /**
     * Ставит груз по центру перед корпусом и неймтейг над грузом.
     *
     * Локальные оси игрока: вперёд = (-sin(yaw), +cos(yaw)),
     * влево = (cos(yaw), +sin(yaw)); высоты считаются от ног.
     * Значения читаются из кэша конфигурации на каждый тик.
     */
    private void follow() {

        if (!carrier.isOnline()) {
            return;
        }

        Location base = carrier.getLocation();
        double yaw = Math.toRadians(base.getYaw());

        double forward = config.getHandsForward();
        double left = config.getHandsLeft();

        double x = base.getX()
                - Math.sin(yaw) * forward
                + Math.cos(yaw) * left;
        double z = base.getZ()
                + Math.cos(yaw) * forward
                + Math.sin(yaw) * left;
        double y = base.getY() + config.getHandsHeight();

        Location hands = new Location(base.getWorld(), x, y, z);

        display.teleport(hands);

        hands.setY(y + config.getNameHeight());
        nameTag.teleport(hands);

    }
}
