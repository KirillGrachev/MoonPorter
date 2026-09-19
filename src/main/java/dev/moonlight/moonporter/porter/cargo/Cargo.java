package dev.moonlight.moonporter.porter.cargo;

import dev.moonlight.moonporter.config.PorterTier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Неизменяемое описание груза, который несёт игрок.
 * Собирается один раз в момент выдачи и дальше только передаётся по ссылкам.
 *
 * @param material    материал визуализации груза
 * @param tier        уровень груза с наградой и весом
 * @param displayName имя груза с уже подставленными плейсхолдерами
 * @param origin      точка выдачи груза, может быть null при создании в тестах
 */
public record Cargo(@NotNull Material material,
                    @NotNull PorterTier tier,
                    @NotNull String displayName,
                    Location origin) {

    /**
     * Проверяет, находится ли игрок в радиусе сдачи от точки выдачи.
     * Принимает уже возведённый в квадрат радиус: сравнение с
     * distanceSquared не извлекает корень в горячем пути.
     *
     * @param player        проверяемый игрок
     * @param radiusSquared квадрат радиуса в блоках, 0 отключает проверку
     * @return true если проверка пройдена
     */
    public boolean isWithinRadius(@NotNull Player player, double radiusSquared) {

        if (radiusSquared <= 0.0D || origin == null) {
            return true;
        }

        Location current = player.getLocation();

        if (current.getWorld() == null || origin.getWorld() == null) {
            return true;
        }

        if (!current.getWorld().equals(origin.getWorld())) {
            return false;
        }

        return current.distanceSquared(origin) <= radiusSquared;

    }
}
