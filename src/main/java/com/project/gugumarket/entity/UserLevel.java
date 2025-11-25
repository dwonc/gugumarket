package com.project.gugumarket.entity;

public enum UserLevel {
    EGG("알", 0, 2, "🥚"),
    BABY_BIRD("새끼새", 3, 9, "🐣"),
    TEEN_BIRD("사춘기새", 10, 29, "🐥"),
    ADULT_BIRD("성체인새", 30, Integer.MAX_VALUE, "🦅");

    private final String displayName;
    private final int minTransactions;
    private final int maxTransactions;
    private final String emoji;

    UserLevel(String displayName, int minTransactions, int maxTransactions, String emoji) {
        this.displayName = displayName;
        this.minTransactions = minTransactions;
        this.maxTransactions = maxTransactions;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinTransactions() {
        return minTransactions;
    }

    public int getMaxTransactions() {
        return maxTransactions;
    }

    public String getEmoji() {
        return emoji;
    }

    public static UserLevel fromTransactionCount(int count) {
        if (count >= ADULT_BIRD.minTransactions) {
            return ADULT_BIRD;
        } else if (count >= TEEN_BIRD.minTransactions) {
            return TEEN_BIRD;
        } else if (count >= BABY_BIRD.minTransactions) {
            return BABY_BIRD;
        } else {
            return EGG;
        }
    }

    public int getTransactionsToNextLevel(int currentCount) {
        if (this == ADULT_BIRD) {
            return 0;
        }
        UserLevel nextLevel = UserLevel.fromTransactionCount(this.maxTransactions + 1);
        return nextLevel.minTransactions - currentCount;
    }
}