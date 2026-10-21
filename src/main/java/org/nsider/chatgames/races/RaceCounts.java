package org.nsider.chatgames.races;

/**
 * How many items one crafting click makes, for the Craft race.
 *
 * <p>Bukkit reports a craft once per click, with the recipe's result. A shift-click
 * crafts as many times as the ingredients allow, limited by room in the inventory;
 * other clicks craft once, or not at all if the cursor can't take the result.</p>
 */
public final class RaceCounts {

    private RaceCounts() {}

    /**
     * @param resultAmount   items one craft makes
     * @param fewestIngredient the smallest stack in the crafting grid (how many crafts the ingredients allow)
     * @param room           how many of the result fit in the inventory
     */
    public static int shiftCrafted(int resultAmount, int fewestIngredient, int room) {
        if (resultAmount <= 0 || fewestIngredient <= 0 || room <= 0) return 0;
        // The last craft still happens when only part of its result fits (the rest stays in the grid's output)
        int craftsWithRoom = (room + resultAmount - 1) / resultAmount;
        return Math.min(fewestIngredient, craftsWithRoom) * resultAmount;
    }

    /**
     * @param resultAmount items one craft makes
     * @param cursorEmpty  nothing is on the cursor
     * @param cursorSame   the cursor holds the same item as the result
     * @param cursorAmount how many items are on the cursor
     * @param maxStack     the result's maximum stack size
     */
    public static int clickCrafted(int resultAmount, boolean cursorEmpty, boolean cursorSame, int cursorAmount, int maxStack) {
        if (resultAmount <= 0) return 0;
        if (cursorEmpty) return resultAmount;
        return cursorSame && cursorAmount + resultAmount <= maxStack ? resultAmount : 0;
    }
}
