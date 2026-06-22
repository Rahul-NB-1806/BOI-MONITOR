package com.boi.monitor.voice;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoiceEngine implements TextToSpeech.OnInitListener {

    private static final String TAG = "VoiceEngine";

    private static volatile VoiceEngine instance;

    private TextToSpeech tts;
    private final AtomicBoolean isReady     = new AtomicBoolean(false);
    private final AtomicBoolean isSpeaking  = new AtomicBoolean(false);
    private volatile boolean initFailed = false;
    private boolean retryPending = false;
    private static final int RETRY_DELAY_MS = 5000;

    private String pendingText = null;
    private Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private VoiceEngine() {}

    public static VoiceEngine getInstance() {
        if (instance == null) {
            synchronized (VoiceEngine.class) {
                if (instance == null) instance = new VoiceEngine();
            }
        }
        return instance;
    }

    public void init(Context context) {
        if (tts != null) return;
        this.appContext = context.getApplicationContext();
        tts = new TextToSpeech(appContext, this);

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId)  { isSpeaking.set(true); }
            @Override public void onDone(String utteranceId)   { isSpeaking.set(false); }
            @Override public void onError(String utteranceId)  {
                isSpeaking.set(false);
                Log.e(TAG, "TTS error for: " + utteranceId);
            }
        });
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isReady.set(false);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int langResult = tts.setLanguage(new Locale("en", "IN"));
            if (langResult == TextToSpeech.LANG_MISSING_DATA
                    || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }
            tts.setSpeechRate(0.95f);
            tts.setPitch(1.0f);
            initFailed = false;
            retryPending = false;
            isReady.set(true);
            Log.i(TAG, "TTS engine ready");

            if (pendingText != null) {
                speakNow(pendingText);
                pendingText = null;
            }
        } else {
            initFailed = true;
            Log.e(TAG, "TTS initialization failed with status: " + status);
            if (!retryPending && appContext != null) {
                retryPending = true;
                mainHandler.postDelayed(() -> {
                    tts = null;
                    init(appContext);
                }, RETRY_DELAY_MS);
            }
        }
    }

    public void announceUpiCredit(long paise) {
        if (paise <= 0) return;
        String amountStr = formatAmount(paise);
        String text = "Received " + amountStr + " rupees through UPI.";
        speak(text);
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) return;

        if (!isReady.get()) {
            Log.d(TAG, "TTS not ready yet, queuing: " + text);
            pendingText = text;
            return;
        }
        speakNow(text);
    }

    public boolean isReady() { return isReady.get(); }

    public boolean isAvailable() { return !initFailed && isReady.get(); }

    public static boolean hasTtsEngine(Context context) {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        return context.getPackageManager().resolveActivity(intent, 0) != null;
    }

    public String getLastError() {
        if (initFailed) return "TTS engine initialization failed - no TTS engine may be installed";
        if (!isReady.get()) return "TTS still initializing";
        return null;
    }

    private void speakNow(String text) {
        if (tts == null || !isReady.get()) return;

        // TTS speak must run on the main thread for reliable audio output
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> speakNow(text));
            return;
        }

        String utteranceId = "boi_upi_" + System.currentTimeMillis();
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        Log.i(TAG, "TTS speaking: " + text);
    }

    private String formatAmount(long paise) {
        long rupees = paise / 100;
        long remainingPaise = paise % 100;

        if (remainingPaise > 0) {
            return rupees + " and " + remainingPaise + " paise";
        }
        return String.valueOf(rupees);
    }
}
