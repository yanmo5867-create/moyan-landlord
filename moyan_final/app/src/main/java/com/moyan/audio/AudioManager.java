package com.moyan.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.moyan.engine.SettingsManager;
import com.moyan.model.Card;
import com.moyan.model.CardType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AudioManager {

    private Context context;
    private SettingsManager settings;
    private SoundPool soundPool;
    private Map<String, Integer> soundMap;
    private TextToSpeech tts;
    private boolean ttsReady;
    private int soundVolume;
    private int ttsVolume;

    // 音效ID常量
    public static final String SFX_SINGLE = "single";
    public static final String SFX_PAIR = "pair";
    public static final String SFX_TRIPLE = "triple";
    public static final String SFX_STRAIGHT = "straight";
    public static final String SFX_PAIR_STRAIGHT = "pair_straight";
    public static final String SFX_AIRPLANE = "airplane";
    public static final String SFX_BOMB = "bomb";
    public static final String SFX_ROCKET = "rocket";
    public static final String SFX_BID = "bid";
    public static final String SFX_WIN = "win";
    public static final String SFX_LOSE = "lose";
    public static final String SFX_FIFTYK = "fiftyk";
    public static final String SFX_CLICK = "click";
    public static final String SFX_ALARM = "alarm"; // 倒计时

    public AudioManager(Context ctx, SettingsManager s) {
        this.context = ctx;
        this.settings = s;
        this.soundVolume = 80;
        this.ttsVolume = 80;
        this.ttsReady = false;

        // 初始化SoundPool
        soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(
                        new android.media.AudioAttributes.Builder()
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(android.media.AudioAttributes.USAGE_GAME)
                                .build()
                ).build();

        soundMap = new HashMap<>();
        // 用程序生成的音调作为兜底音效
        // 实际项目中应放入 res/raw/ 下的音频文件

        // 初始化TTS
        tts = new TextToSpeech(ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINA);
                if (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsReady = true;
                    tts.setSpeechRate(1.1f);
                    tts.setPitch(1.0f);
                }
            }
        });
    }

    /**
     * 播放出牌音效
     */
    public void playCardSound(List<Card> cards) {
        if (!settings.isSoundEnabled()) return;
        if (cards == null || cards.isEmpty()) return;

        // 通过牌型判断播放哪个音效
        String sfx = identifySoundEffect(cards);
        playSound(sfx);
    }

    /**
     * 根据牌型识别音效
     */
    private String identifySoundEffect(List<Card> cards) {
        // 简化判断：根据张数和牌值
        if (cards.size() == 1) return SFX_SINGLE;
        if (cards.size() == 2) {
            // 王炸？
            boolean hasSmall = false, hasBig = false;
            for (Card c : cards) {
                if (c.getValue() == 16) hasSmall = true;
                if (c.getValue() == 17) hasBig = true;
            }
            if (hasSmall && hasBig) return SFX_ROCKET;
            return SFX_PAIR;
        }
        if (cards.size() == 3) return SFX_TRIPLE;
        if (cards.size() >= 5) {
            // 顺子判断
            boolean isStraight = true;
            for (Card c : cards) {
                if (c.getValue() >= 15) isStraight = false;
            }
            if (isStraight) return SFX_STRAIGHT;
        }
        // 检查是否有4张（炸弹）
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Card c : cards) {
            countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
        }
        for (int cnt : countMap.values()) {
            if (cnt == 4) return SFX_BOMB;
        }
        return SFX_SINGLE;
    }

    /**
     * 播放音效
     */
    private void playSound(String name) {
        if (!settings.isSoundEnabled()) return;
        // 兜底：用ToneGenerator生成提示音
        try {
            android.media.ToneGenerator tg = new android.media.ToneGenerator(
                    AudioManager.STREAM_MUSIC, soundVolume / 2);
            int tone = getToneForSound(name);
            tg.startTone(tone, 150);
        } catch (Exception e) {
            // 忽略
        }
    }

    private int getToneForSound(String name) {
        switch (name) {
            case SFX_BOMB: return android.media.ToneGenerator.TONE_PROP_BEEP2;
            case SFX_ROCKET: return android.media.ToneGenerator.TONE_PROP_ACK;
            case SFX_STRAIGHT: return android.media.ToneGenerator.TONE_CDMA_HIGH_L;
            case SFX_PAIR_STRAIGHT: return android.media.ToneGenerator.TONE_CDMA_HIGH_PBX_L;
            case SFX_AIRPLANE: return android.media.ToneGenerator.TONE_CDMA_HIGH_SS;
            case SFX_WIN: return android.media.ToneGenerator.TONE_CDMA_CONFIRM;
            case SFX_LOSE: return android.media.ToneGenerator.TONE_CDMA_REORDER;
            default: return android.media.ToneGenerator.TONE_PROP_BEEP;
        }
    }

    /**
     * 语音播报出牌内容
     */
    public void speakPlayedCards(String playerName, List<Card> cards) {
        if (!ttsReady || !settings.isSoundEnabled()) return;

        String text = buildPlayText(playerName, cards);
        if (text.isEmpty()) return;

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "play_card");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    }

    /**
     * 构建播报文本
     */
    private String buildPlayText(String playerName, List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        sb.append(playerName).append("打出");

        if (cards == null || cards.isEmpty()) return "";

        int size = cards.size();
        boolean isRocket = false;
        if (size == 2) {
            boolean s = false, b = false;
            for (Card c : cards) {
                if (c.getValue() == 16) s = true;
                if (c.getValue() == 17) b = true;
            }
            if (s && b) { sb.append("王炸"); isRocket = true; }
        }

        if (!isRocket) {
            // 简化播报
            Map<Integer, Integer> countMap = new HashMap<>();
            for (Card c : cards) {
                countMap.put(c.getValue(), countMap.getOrDefault(c.getValue(), 0) + 1);
            }

            if (countMap.size() == 1) {
                int v = cards.get(0).getValue();
                int cnt = countMap.get(v);
                sb.append(cnt).append("张");
                if (v == 16) sb.append("小王");
                else if (v == 17) sb.append("大王");
                else sb.append(valueName(v));
            } else {
                sb.append(size).append("张牌");
            }
        }

        return sb.toString();
    }

    private String valueName(int v) {
        if (v == 11) return "J";
        if (v == 12) return "Q";
        if (v == 13) return "K";
        if (v == 14) return "A";
        if (v == 15) return "2";
        return String.valueOf(v);
    }

    /**
     * 播放倒计时警告
     */
    public void playTimerWarning() {
        if (!settings.isSoundEnabled()) return;
        try {
            android.media.ToneGenerator tg = new android.media.ToneGenerator(
                    AudioManager.STREAM_MUSIC, 100);
            tg.startTone(android.media.ToneGenerator.TONE_CDMA_HIGH_L, 100);
        } catch (Exception ignored) {}
    }

    /**
     * 播放胜利音效
     */
    public void playWin() {
        if (!settings.isSoundEnabled()) return;
        try {
            android.media.ToneGenerator tg = new android.media.ToneGenerator(
                    AudioManager.STREAM_MUSIC, 100);
            tg.startTone(android.media.ToneGenerator.TONE_CDMA_CONFIRM, 500);
        } catch (Exception ignored) {}
    }

    /**
     * 播放失败音效
     */
    public void playLose() {
        if (!settings.isSoundEnabled()) return;
        try {
            android.media.ToneGenerator tg = new android.media.ToneGenerator(
                    AudioManager.STREAM_MUSIC, 60);
            tg.startTone(android.media.ToneGenerator.TONE_CDMA_REORDER, 500);
        } catch (Exception ignored) {}
    }

    /**
     * 释放资源
     */
    public void release() {
        if (soundPool != null) soundPool.release();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
