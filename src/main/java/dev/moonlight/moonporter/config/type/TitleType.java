package dev.moonlight.moonporter.config.type;

/**
 * Типы титулов, отправляемых игроку.
 * Имена соответствуют секциям messages в config.yml (TIMEOUT -> "timeout").
 */
public enum TitleType {

    /** Груз успешно взят */
    PICKUP_SUCCESS("pickup.success"),

    /** Игрок уже несёт груз */
    PICKUP_DENIED("pickup.denied"),

    /** Мир игрока не входит в allowed_worlds */
    PICKUP_WRONG_WORLD("pickup.wrong_world"),

    /** В инвентаре нет свободного слота под груз */
    PICKUP_NO_SPACE("pickup.no_space"),

    /** Груз нельзя выбросить */
    DROP_DENIED("drop.denied"),

    /** Груз успешно сдан */
    DELIVERY_SUCCESS("delivery.success"),

    /** Сдача в мире не из allowed_worlds */
    DELIVERY_WRONG_WORLD("delivery.wrong_world"),

    /** Точка сдачи вне региона или радиуса */
    DELIVERY_WRONG_POINT("delivery.wrong_point"),

    /** Игрок включил полёт */
    FLIGHT("flight"),

    /** Игрок сменил игровой режим */
    GAMEMODE("gamemode"),

    /** Истекло время доставки */
    TIMEOUT("timeout"),

    /** Действует задержка между переносками */
    COOLDOWN("cooldown"),

    /** Недостаточно прав */
    NO_PERMISSION("no_permission");

    private final String path;

    TitleType(String path) {
        this.path = path;
    }

    /**
     * Возвращает путь к секции титула в config.yml.
     *
     * @return имя секции сообщений
     */
    public String getPath() {
        return path;
    }
}
