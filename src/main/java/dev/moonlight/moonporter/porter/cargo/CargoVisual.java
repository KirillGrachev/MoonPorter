package dev.moonlight.moonporter.porter.cargo;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Визуальное представление груза, прикреплённое к игроку.
 *
 * Интерфейс намеренно отделён от логики переноски: текущая реализация
 * использует сущность Bukkit, при необходимости её можно заменить
 * на пакетную (NMS) реализацию без изменения остального кода плагина.
 */
public interface CargoVisual {

    /**
     * Прикрепляет визуализацию к игроку.
     *
     * @param player несущий игрок
     */
    void attach(@NotNull Player player);

    /**
     * Удаляет визуализацию из мира.
     * Вызов обязан быть идемпотентным.
     */
    void remove();

    /**
     * Жива ли ещё сущность визуализации.
     *
     * @return true если сущность существует в мире
     */
    boolean isAlive();

    /**
     * Применяет имя груза к визуализации.
     *
     * @param name    окрашенное имя с подставленными плейсхолдерами
     * @param visible показывать ли неймтейг (settings.cargo.name_visible)
     */
    void applyName(@NotNull String name, boolean visible);
}
