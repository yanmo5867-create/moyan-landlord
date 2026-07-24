package com.moyan;

import android.app.Application;
import android.content.Context;

import com.moyan.audio.AudioManager;
import com.moyan.audio.VibrationManager;
import com.moyan.engine.CardCounter;
import com.moyan.engine.CoinRankManager;
import com.moyan.engine.RefreshRateManager;
import com.moyan.engine.SettingsManager;

public class MoyanApp extends Application {

    private static MoyanApp instance;
    private SettingsManager settings;
    private CoinRankManager coinManager;
    private CardCounter cardCounter;
    private AudioManager audioManager;
    private VibrationManager vibrationManager;
    private RefreshRateManager refreshRateManager;
    private StatsManager stats;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        settings = new SettingsManager(this);
        coinManager = new CoinRankManager(this);
        cardCounter = new CardCounter();
        audioManager = new AudioManager(this, settings);
        vibrationManager = new VibrationManager(this, settings);
        refreshRateManager = new RefreshRateManager(this);
        stats = new StatsManager(this);
    }

    public static MoyanApp getInstance() {
        return instance;
    }

    public Context getContext() {
        return getApplicationContext();
    }

    public SettingsManager getSettings() { return settings; }
    public CoinRankManager getCoinManager() { return coinManager; }
    public CardCounter getCardCounter() { return cardCounter; }
    public AudioManager getAudioManager() { return audioManager; }
    public VibrationManager getVibrationManager() { return vibrationManager; }
    public RefreshRateManager getRefreshRateManager() { return refreshRateManager; }
    public StatsManager getStats() { return stats; }
}
