package com.moyan;

import android.content.Context;
import android.content.SharedPreferences;

public class StatsManager {
    private static final String PREFS = "moyan_stats";
    private SharedPreferences sp;
    private int totalGames;
    private int wins;
    private int springCount;
    private int bombCount;
    private int highestMultiplier;

    public StatsManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        totalGames = sp.getInt("totalGames", 0);
        wins = sp.getInt("wins", 0);
        springCount = sp.getInt("springCount", 0);
        bombCount = sp.getInt("bombCount", 0);
        highestMultiplier = sp.getInt("highestMultiplier", 0);
    }

    private void save() {
        sp.edit()
            .putInt("totalGames", totalGames)
            .putInt("wins", wins)
            .putInt("springCount", springCount)
            .putInt("bombCount", bombCount)
            .putInt("highestMultiplier", highestMultiplier)
            .apply();
    }

    public void recordGame(boolean won, int multiplier) {
        totalGames++;
        if (won) wins++;
        if (multiplier > highestMultiplier) highestMultiplier = multiplier;
        save();
    }

    public void recordSpring() { springCount++; save(); }
    public void recordBomb() { bombCount++; save(); }

    public int getWinRate() {
        if (totalGames == 0) return 0;
        return (wins * 100) / totalGames;
    }

    public int getTotalGames() { return totalGames; }
    public int getWins() { return wins; }
    public int getSpringCount() { return springCount; }
    public int getBombCount() { return bombCount; }
    public int getHighestMultiplier() { return highestMultiplier; }
}
