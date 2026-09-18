package dev.moonlight.moonporter.service;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Абстракция проверки регионов.
 *
 * Позволяет плагину работать без WorldGuard: реализация по умолчанию
 * всегда отвечает "разрешено", а WorldGuardRegionProvider подключается
 * только если плагин найден в списке установленных.
 */
public interface RegionProvider {

    /**
     * Название провайдера для лога.
     *
     * @return имя реализации
     */
    @NotNull String getName();

    /**
     * Находится ли точка хотя бы в одном из перечисленных регионов.
     *
     * @param location проверяемая точка
     * @param regions  список имён регионов
     * @return true если точка попадает в один из регионов
     */
    boolean isInAnyRegion(@NotNull Location location, @NotNull List<String> regions);
}
