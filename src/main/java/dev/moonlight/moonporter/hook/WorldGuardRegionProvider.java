package dev.moonlight.moonporter.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.moonlight.moonporter.service.RegionProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Проверка регионов через WorldGuard.
 *
 * Класс загружается только если WorldGuard установлен,
 * поэтому его импорты не могут обрушить плагин на сервере без него.
 */
public final class WorldGuardRegionProvider implements RegionProvider {

    @Override
    public @NotNull String getName() {
        return "WorldGuard";
    }

    @Override
    public boolean isInAnyRegion(@NotNull Location location, @NotNull List<String> regions) {

        if (regions.isEmpty()) {
            return true;
        }

        RegionQuery query = createQuery(location.getWorld());

        if (query == null) {
            return true;
        }

        com.sk89q.worldedit.util.Location position = BukkitAdapter.adapt(location);
        ApplicableRegionSet applicable = query.getApplicableRegions(position);

        for (ProtectedRegion region : applicable) {

            if (regions.contains(region.getId())) {
                return true;
            }

        }

        return false;

    }

    /**
     * Создаёт запрос регионов для мира.
     * WorldGuard возвращает null для миров, где регионы отключены.
     *
     * @param world мир точки
     * @return запрос либо null
     */
    private @Nullable RegionQuery createQuery(@Nullable World world) {

        if (world == null) {
            return null;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

        if (regionManager == null) {
            return null;
        }

        return container.createQuery();

    }
}
