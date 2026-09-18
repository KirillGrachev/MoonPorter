package dev.moonlight.moonporter.config.type;

/**
 * Причина отмены активной переноски.
 * Каждая причина связана с собственным титулом из config.yml.
 */
public enum CancelReason {

    /** Груз успешно сдан */
    DELIVERED(TitleType.DELIVERY_SUCCESS),

    /** Игрок включил полёт */
    FLYING(TitleType.FLIGHT),

    /** Игрок вышел из режима выживания */
    GAMEMODE(TitleType.GAMEMODE),

    /** Истекло время доставки */
    EXPIRED(TitleType.TIMEOUT),

    /** Игрок вышел с сервера — уведомление не отправляется */
    QUIT(null),

    /** Игрок умер — уведомление не отправляется */
    DEATH(null),

    /** Сущность груза уничтожена извне — уведомление не отправляется */
    CARGO_LOST(null),

    /** Остановка сервера или /moonporter reload — уведомление не отправляется */
    SHUTDOWN(null);

    private final TitleType titleType;

    CancelReason(TitleType titleType) {
        this.titleType = titleType;
    }

    /**
     * Возвращает титул для уведомления игрока.
     *
     * @return тип титула либо null, если уведомление не требуется
     */
    public TitleType getTitleType() {
        return titleType;
    }

    /**
     * Нужно ли уведомлять игрока об отмене.
     *
     * @return true если причина подразумевает титул
     */
    public boolean isSilent() {
        return titleType == null;
    }
}
