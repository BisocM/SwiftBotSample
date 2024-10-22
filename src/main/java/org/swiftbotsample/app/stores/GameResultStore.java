package org.swiftbotsample.app.stores;

public class GameResultStore {
    private static int lastGameScore = 0;
    private static int lastGameMaxScore = 0;

    public static synchronized void setLastGameScore(int score) {
        lastGameScore = score;
    }

    public static synchronized int getLastGameScore() {
        return lastGameScore;
    }

    public static synchronized void setLastGameMaxScore(int maxScore) {
        lastGameMaxScore = maxScore;
    }

    public static synchronized int getLastGameMaxScore() {
        return lastGameMaxScore;
    }
}