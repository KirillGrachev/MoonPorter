package dev.moonlight.moonporter.service;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Реализация-заглушка для серверов без WorldGuard.
 * Региональные ограничения в этом режиме не применяются.
 */
public final class DefaultRegionProvider implements RegionProvider {

    @Override
    public @NotNull String getName() {
        return "none";
    }

    @Override
    public boolean isInAnyRegion(@NotNull Location location, @NotNull List<String> regions) {
        return true;
    }
}
