package com.moyan.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 音频管理器
 * 三通道：背景音乐 / 音效 / 语音播报
 * 支持 TTS 语音播报出牌
 */
public class AudioManager implements TextToSpeech.OnInitListener {

    private Context context;
    private SoundPool soundPool;
    private Map<String, Integer> soundMap;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    // 音量
    private float bgmVolume = 0.5f;
    private float sfxVolume = 0.8f;
    private float voiceVolume = 0.8f;

    // 音效名称常量
    public static final String SFX_SINGLE = "single";
    public static final String SFX_PAIR = "pair";
    public static final String SFX_TRIPLE = "triple";
    public static final String SFX_STRAIGHT = "straight";
    public static final String SFX_PLANE = "plane";
    public static final String SFX_STRAIGHT_PAIR = "straight_pair";
    public static final String SFX_BOMB = "bomb";
    public static final String SFX_ROCKET = "rocket";
    public static final String SFX_CALL = "call";
    public static final String SFX_WIN = "win";
    public static final String SFX_LOSE = "lose";
    public static final String SFX_FIFTYK = "fiftyk";

    // 语音播报映射
    private Map<String, String> cardNameMap;

    public AudioManager(Context context) {
        this.context = context;
        initSoundPool();
        initTTS();
        initCardNames();
    }

    private void initSoundPool() {
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(8);
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int sampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) != null ?
                Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)) : 44100;
        builder.setAudioAttributes(
                new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_GAME)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
        );
        soundPool = builder.build();
        soundMap = new HashMap<>();

        // 注：实际音效文件需放入 res/raw/ 目录
        // 这里用程序化方式生成短音效
        generateSyntheticSounds();
    }

    /**
     * 用程序化方式生成简易音效（无需外部音频文件）
     * 不同牌型不同频率和时长
     */
    private void generateSyntheticSounds() {
        // 由于无法在运行时创建音频文件，我们用 MediaPlayer 播放方式替代
        // 实际项目中请将音效文件放入 res/raw/
        // 这里先注册空的映射，playSfx 中会做兜底处理
    }

    private void initTTS() {
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 回退到英文
                tts.setLanguage(Locale.US);
            }
            tts.setSpeechRate(1.2f);
            ttsReady = true;
        }
    }

    private void initCardNames() {
        cardNameMap = new HashMap<>();
        for (int i = 3; i <= 10; i++) cardNameMap.put(String.valueOf(i), String.valueOf(i));
        cardNameMap.put("11", "J");
        cardNameMap.put("12", "Q");
        cardNameMap.put("13", "K");
        cardNameMap.put("14", "A");
        cardNameMap.put("15", "2");
        cardNameMap.put("16", "小王");
        cardNameMap.put("17", "大王");
    }

    // ========== 公共接口 ==========

    /** 播放牌型音效 */
    public void playCardTypeSfx(String type) {
        // 使用 TTS 播放简短音效替代
        // 实际项目应加载 res/raw/ 中的音频文件
        playToneForType(type);
    }

    /** 播放语音播报 */
    public void speak(String text) {
        if (ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "moyan_utterance");
        }
    }

    /** 播报出牌 */
    public void announcePlay(int player, String cardTypeDesc, String cardDesc) {
        String[] names = {"我", "上方玩家", "下方玩家"};
        String text = names[player] + "打出" + cardTypeDesc + cardDesc;
        speak(text);
    }

    /** 播报叫分 */
    public void announceCall(int player, int score) {
        String[] names = {"我", "上方玩家", "下方玩家"};
        String text = names[player] + (score == 0 ? "不叫" : "叫" + score + "分");
        speak(text);
    }

    /** 播报地主 */
    public void announceLandlord(int player) {
        String[] names = {"我", "上方玩家", "下方玩家"};
        speak(names[player] + "成为地主");
    }

    /** 播报过牌 */
    public void announcePass(int player) {
        String[] names = {"我", "上方玩家", "下方玩家"};
        speak(names[player] + "过牌");
    }

    // ========== 音量控制 ==========

    public void setBgmVolume(float v) { bgmVolume = clamp(v); }
    public void setSfxVolume(float v) { sfxVolume = clamp(v); }
    public void setVoiceVolume(float v) { voiceVolume = clamp(v); }

    public float getBgmVolume() { return bgmVolume; }
    public float getSfxVolume() { return sfxVolume; }
    public float getVoiceVolume() { return voiceVolume; }

    private float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    // ========== 程序化音效（兜底） ==========

    private void playToneForType(String type) {
        // 用不同频率模拟不同牌型的音效
        // 实际项目应替换为 res/raw/ 中的真实音效文件
        int freq = 440;
        int duration = 200;
        switch (type) {
            case SFX_SINGLE: freq = 523; duration = 150; break;
            case SFX_PAIR: freq = 587; duration = 200; break;
            case SFX_TRIPLE: freq = 659; duration = 250; break;
            case SFX_STRAIGHT: freq = 698; duration = 400; break;
            case SFX_STRAIGHT_PAIR: freq = 740; duration = 450; break;
            case SFX_PLANE: freq = 784; duration = 500; break;
            case SFX_BOMB: freq = 330; duration = 600; break; // 低沉
            case SFX_ROCKET: freq = 220; duration = 800; break; // 最低
            case SFX_FIFTYK: freq = 880; duration = 500; break;
            case SFX_WIN: freq = 1047; duration = 600; break;
            case SFX_LOSE: freq = 196; duration = 600; break;
        }
        // 使用 ToneGenerator 生成提示音
        try {
            android.media.ToneGenerator tg = new android.media.ToneGenerator(
                    android.media.AudioManager.STREAM_MUSIC, (int)(sfxVolume * 80));
            // 映射到 ToneGenerator 的预定义音调
            int tone = mapFreqToTone(freq);
            tg.startTone(tone, duration);
            new Handler(Looper.getMainLooper()).postDelayed(tg::release, duration + 100);
        } catch (Exception e) {
            // 静默失败
        }
    }

    private int mapFreqToTone(int freq) {
        if (freq >= 1000) return android.media.ToneGenerator.TONE_PROP_BEEP2;
        if (freq >= 800) return android.media.ToneGenerator.TONE_PROP_BEEP;
        if (freq >= 600) return android.media.ToneGenerator.TONE_CDMA_HIGH_L;
        if (freq >= 400) return android.media.ToneGenerator.TONE_CDMA_MED_L;
        return android.media.ToneGenerator.TONE_CDMA_LOW_L;
    }

    // ========== 资源释放 ==========

    public void release() {
        if (soundPool != null) soundPool.release();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
