package org.nsider.chatgames.utils.management;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rounds times for display. Games and races measure their own elapsed time.
 */
public final class TimeUtils {

    private TimeUtils() {}

    /**
     * Rounds half to even, as DecimalFormat did; unlike DecimalFormat it doesn't depend on the
     * server's locale, whose digits or separators Double.parseDouble may not read back.
     *
     * @param decimals how many decimal places to keep
     * @param val the value to round
     * @return the rounded value
     */
    private static double roundToDecimals(int decimals, double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return val;
        return new BigDecimal(val).setScale(decimals, RoundingMode.HALF_EVEN).doubleValue();
    }

    /**
     * Rounds a given value to 1 decimal place.
     *
     * @param val the value to round
     * @return the value rounded to 1 decimal place
     */
    public static double roundTo1Decimal(double val) {
        return roundToDecimals(1, val);
    }

    /**
     * Rounds a given value to 2 decimal places.
     *
     * @param val the value to round
     * @return the value rounded to 2 decimal places
     */
    public static double roundTo2Decimals(double val) {
        return roundToDecimals(2, val);
    }
}
