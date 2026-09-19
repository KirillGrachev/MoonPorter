package dev.moonlight.moonporter.porter.cargo;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.config.MoonPorterConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Груз в двух руках: BlockDisplay без физики перед корпусом игрока
 * и невидимый маркер-armor stand для неймтейга над грузом.
 * Armor stand не является грузом: он только несёт имя, потому что
 * у BlockDisplay неймтейг прилипает к центру блока.
 *
 * Позиция строится в локальной системе координат игрока
 * (вперёд / высота / влево) и читается из конфигурации каждый тик:
 * правка конфига или /mporter visual применяются к несомому грузу.
 *
 * Две особенности ядра компенсируются автоматически:
 * 1. Блок дисплея рендерится повёрнутым по yaw сущности, поэтому
 *    дисплей всегда телепортируется с yaw 0 — груз стоит ровно,
 *    как его и держат, на любом повороте игрока.
 * 2. Рендер-якорь ядра смещает блок на константу относительно позиции
 *    сущности. Смещение измеряется один раз при спавне через
 *    bounding box дисплея и вычитается каждый тик. Если ядро не
 *    отражает рендер в bounding box (объём нулевой), поправка
 *    остаётся нулевой и всё работает как прежде.
 *
 * Таск живёт только пока жив груз. Требует ядро 1.19.4+:
 * на старых ядрах фабрика вернёт FallingBlockVisual.
 */
public final class BlockDisplayVisual implements CargoVisual {

    private static final long PERIOD_TICKS = 1L;

    private final MoonPorter plugin;
    private final MoonPorterConfig config;
    private final BlockDisplay display;
    private final ArmorStand nameTag;
    private final UUID carrierId;

    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;

    private @Nullable BukkitTask task;

    public BlockDisplayVisual(@NotNull MoonPorter plugin,
                              @NotNull MoonPorterConfig config,
                              @NotNull Player player,
                              @NotNull Cargo cargo) {

        this.plugin = plugin;
        this.config = config;
        this.carrierId = player.getUniqueId();

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

        double[] measured = calibrate(player);

        this.offsetX = measured[0];
        this.offsetY = measured[1];
        this.offsetZ = measured[2];

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
     * Измеряет, куда ядро фактически рисует блок относительно позиции сущности.
     * Дисплей ставится в известную точку с yaw 0, после чего центр
     * его bounding box сравнивается с этой точкой.
     *
     * @param player носитель груза
     * @return поправка рендера по осям мира либо нули, если измерить не удалось
     */
    private double @NotNull [] calibrate(@NotNull Player player) {

        Location probe = player.getLocation();

        probe.setYaw(0.0F);
        probe.setPitch(0.0F);

        display.teleport(probe);

        BoundingBox box = display.getBoundingBox();

        if (box == null || box.getVolume() <= 0.0D) {
            return new double[]{0.0D, 0.0D, 0.0D};
        }

        return new double[]{
                box.getCenterX() - probe.getX(),
                box.getCenterY() - probe.getY(),
                box.getCenterZ() - probe.getZ()
        };

    }

    /**
     * Ставит груз по центру перед корпусом и неймтейг над грузом.
     *
     * Локальные оси игрока: вперёд = (-sin(yaw), +cos(yaw)),
     * влево = (cos(yaw), +sin(yaw)); высоты считаются от ног.
     * Дисплей телепортируется с yaw 0 и вычтенной поправкой рендера,
     * поэтому на любых поворотах груз стоит ровно и на своём месте.
     */
    private void follow() {

        Player player = Bukkit.getPlayer(carrierId);

        if (player == null || !player.isOnline()) {
            return;
        }

        Location base = player.getLocation();
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

        display.teleport(new Location(base.getWorld(),
                x - offsetX,
                y - offsetY,
                z - offsetZ,
                0.0F,
                0.0F));

        nameTag.teleport(new Location(base.getWorld(), x, y + config.getNameHeight(), z));

    }
}
