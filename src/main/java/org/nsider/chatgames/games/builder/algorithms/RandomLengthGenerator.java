package org.nsider.chatgames.games.builder.algorithms;

public class RandomLengthGenerator {

    public static int getRandomLength(String range) {
        int[] bounds = parseRange(range);
        return generateRandomLength(bounds[0], bounds[1]);
    }

    private static int[] parseRange(String range) {
        String[] split = range.split("-");
        int lower = Integer.parseInt(split[0]);
        int higher = Integer.parseInt(split[1]);
        return new int[]{lower, higher};
    }

    private static int generateRandomLength(int lower, int higher) {
        return (int) (Math.random() * (higher - lower + 1)) + lower;
    }
}
