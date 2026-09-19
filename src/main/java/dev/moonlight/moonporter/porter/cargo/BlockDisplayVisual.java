package dev.moonlight.moonporter.porter.cargo;

import dev.moonlight.moonporter.MoonPorter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Груз в руках: BlockDisplay без физики, который следует перед игроком
 * на высоте рук. Точка удержания настраивается в settings.cargo.hands_offset.
 *
 * Движение сглажено интерполяцией дисплея, таск живёт только пока жив груз.
 * Требует ядро 1.19.4+: на старых ядрах фабрика вернёт FallingBlockVisual.
 */
public final class BlockDisplayVisual implements CargoVisual {

    private static final long PERIOD_TICKS = 2L;

    private final MoonPorter plugin;
    private final BlockDisplay display;
    private final UUID carrierId;
    private final double forward;
    private final double down;

    private @Nullable BukkitTask task;

    public BlockDisplayVisual(@NotNull MoonPorter plugin,
                              @NotNull Player player,
                              @NotNull Cargo cargo,
                              double forward,
                              double down) {

        this.plugin = plugin;
        this.carrierId = player.getUniqueId();
        this.forward = forward;
        this.down = down;

        this.display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);

        this.display.setBlock(cargo.material().createBlockData());
        this.display.setPersistent(false);
        this.display.setInvulnerable(true);
        this.display.setInterpolationDelay(0);
        this.display.setInterpolationDuration(3);

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

    /**
     * Ставит дисплей перед игроком на высоте рук.
     * Направление взгляда учитывается: груз идёт туда, куда смотрит игрок.
     */
    private void follow() {

        Player player = Bukkit.getPlayer(carrierId);

        if (player == null || !player.isOnline()) {
            return;
        }

        Location hands = player.getEyeLocation();

        hands.add(hands.getDirection().multiply(forward));
        hands.setY(hands.getY() - down);

        display.teleport(hands);

    }
}
