package org.nsider.chatgames.utils.management;


import org.nsider.inventorymanager.extras.Nonnull;

import java.text.DecimalFormat;

/**
 * Rounds times for display. Games and races measure their own elapsed time.
 */
public final class TimeUtils {

    private TimeUtils() {}

    /**
     * Rounds a given value to a specific decimal format pattern.
     *
     * @param pattern the decimal format pattern (e.g., "#.#" for 1 decimal place)
     * @param val the value to round
     * @return the rounded value
     */
    private static double roundToDecimals(@Nonnull String pattern, double val) {
        DecimalFormat df = new DecimalFormat(pattern);
        String num = df.format(val).replace(",", ".");
        return Double.parseDouble(num);
    }

    /**
     * Rounds a given value to 1 decimal place.
     *
     * @param val the value to round
     * @return the value rounded to 1 decimal place
     */
    public static double roundTo1Decimal(double val) {
        return roundToDecimals("#.#", val);
    }

    /**
     * Rounds a given value to 2 decimal places.
     *
     * @param val the value to round
     * @return the value rounded to 2 decimal places
     */
    public static double roundTo2Decimals(double val) {
        return roundToDecimals("#.##", val);
    }
}
