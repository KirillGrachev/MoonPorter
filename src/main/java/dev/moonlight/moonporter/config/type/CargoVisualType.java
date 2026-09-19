package dev.moonlight.moonporter.config.type;

/**
 * Способ отображения груза у игрока.
 */
public enum CargoVisualType {

    /** Падающий блок верхом на игроке: работает на любом ядре 1.16+ */
    HEAD,

    /** BlockDisplay на высоте рук: требует ядро 1.19.4+, иначе автоматом HEAD */
    HANDS
}
