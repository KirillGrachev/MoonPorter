package dev.moonlight.moonporter.config.type;

/**
 * Режим представления груза.
 *
 * HEAD — сущность над головой игрока, инвентарь не трогается.
 * INVENTORY — предмет в инвентаре, сущность не спавнится.
 * BOTH — оба представления одновременно.
 */
public enum CargoMode {

    HEAD,
    INVENTORY,
    BOTH;

    /**
     * Есть ли в режиме сущность над головой.
     *
     * @return true если визуал спавнится
     */
    public boolean hasHead() {
        return this != INVENTORY;
    }

    /**
     * Есть ли в режиме предмет в инвентаре.
     *
     * @return true если груз кладётся в инвентарь
     */
    public boolean hasInventory() {
        return this != HEAD;
    }
}
