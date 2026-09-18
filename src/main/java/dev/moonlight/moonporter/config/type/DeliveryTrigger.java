package dev.moonlight.moonporter.config.type;

/**
 * Способ сдачи груза.
 */
public enum DeliveryTrigger {

    /** Сдача в момент нажатия Shift */
    SNEAK_TOGGLE,

    /** Сдача при удержании Shift: проверяется при переходе игрока между блоками */
    SNEAK_HOLD
}
