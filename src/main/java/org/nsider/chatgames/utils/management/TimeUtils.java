package org.nsider.chatgames.utils.management;


import lombok.Getter;
import lombok.Setter;
import org.nsider.inventorymanager.extras.Nonnull;

import java.text.DecimalFormat;

/**
 * Utility class for managing timers and formatting elapsed time or numeric values with precision.
 */
public class TimeUtils {

    /** The start time of the timer, in milliseconds. */
    private static long startTime = 0;

    /** The stop time of the timer, in milliseconds. */
    private static long stopTime = 0;

    /** The last timer value recorded. */
    @Getter @Setter private static double lastTimer = 0.00;

    /** Indicates whether the timer is currently running. */
    private static boolean running = false;

    /**
     * Starts the timer by recording the current system time in milliseconds.
     */
    public static void startTimer() {
        startTime = System.currentTimeMillis();
        running = true;
    }

    /**
     * Stops the timer by recording the current system time in milliseconds.
     */
    public static void stopTimer() {
        stopTime = System.currentTimeMillis();
        running = false;
    }

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

    /**
     * Retrieves the elapsed time as a formatted string.
     * The format is "seconds.milliseconds" where milliseconds are rounded to two decimal places.
     *
     * @return a formatted string representing the elapsed time
     */
    @Nonnull
    public static String getElapsedTime() {
        long elapsed;

        if (running) {
            elapsed = System.currentTimeMillis() - startTime;
        } else {
            elapsed = stopTime - startTime;
        }

        long seconds = elapsed / 1000;
        long milliseconds = elapsed % 1000;

        // Format milliseconds to two decimal places as a fraction of a second
        String formattedMilliseconds = String.format("%.2f", (double) milliseconds / 1000);

        return seconds + "." + formattedMilliseconds.substring(2);
    }
}
