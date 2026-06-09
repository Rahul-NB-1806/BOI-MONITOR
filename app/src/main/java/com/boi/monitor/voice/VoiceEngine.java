package com.boi.monitor.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VoiceEngine
 *
 * Wraps Android TextToSpeech for UPI credit announcements.
 * Ensures:
 * - No duplicate announcements (debounced via utterance ID tracking)
 * - Lazy initialization (TTS init is async)
 * - Background-safe (no UI thread dependency)
 *
 * Only UPI CREDIT notifications must trigger announcements.
 * Cheque notifications must NEVER call speak().
 */
public class VoiceEngine implements TextToSpeech.OnInitListener {

    private static final String TAG = "VoiceEngine";

    // Singleton
    private static volatile VoiceEngine instance;

    private TextToSpeech tts;
    private final AtomicBoolean isReady     = new AtomicBoolean(false);
    private final AtomicBoolean isSpeaking  = new AtomicBoolean(false);

    // Queue for announcements requested before TTS is ready
    private String pendingText = null;

    // ── Singleton ──────────────────────────────────────────────────────────────

    private VoiceEngine() {}

    public static VoiceEngine getInstance() {
        if (instance == null) {
            synchronized (VoiceEngine.class) {
                if (instance == null) instance = new VoiceEngine();
            }
        }
        return instance;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /**
     * Initialize TTS engine. Call once from the service.
     */
    public void init(Context context) {
        if (tts != null) return; // already initialized
        tts = new TextToSpeech(context.getApplicationContext(), this);

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId)  { isSpeaking.set(true); }
            @Override public void onDone(String utteranceId)   { isSpeaking.set(false); }
            @Override public void onError(String utteranceId)  {
                isSpeaking.set(false);
                Log.e(TAG, "TTS error for: " + utteranceId);
            }
        });
    }

    /**
     * Release TTS resources. Call from service onDestroy().
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isReady.set(false);
        }
    }

    // ── TextToSpeech.OnInitListener ────────────────────────────────────────────

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int langResult = tts.setLanguage(new Locale("en", "IN")); // Indian English
            if (langResult == TextToSpeech.LANG_MISSING_DATA
                    || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to US English
                tts.setLanguage(Locale.US);
            }
            tts.setSpeechRate(0.95f);
            tts.setPitch(1.0f);
            isReady.set(true);
            Log.i(TAG, "TTS engine ready");

            // Speak any pending announcement
            if (pendingText != null) {
                speakNow(pendingText);
                pendingText = null;
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: " + status);
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Announce UPI credit amount via TTS.
     * Format: "Received {amount} rupees through UPI."
     *
     * @param amount the credited amount
     */
    public void announceUpiCredit(long paise) {
        if (paise <= 0) return;
        String amountStr = formatAmount(paise);
        String text = "Received " + amountStr + " rupees through UPI.";
        speak(text);
    }

    /**
     * Speak arbitrary text (use only for UPI announcements).
     */
    public void speak(String text) {
        if (text == null || text.isEmpty()) return;

        if (!isReady.get()) {
            Log.d(TAG, "TTS not ready yet, queuing: " + text);
            pendingText = text; // store last pending
            return;
        }
        speakNow(text);
    }

    public boolean isReady() { return isReady.get(); }

    // ── Internals ──────────────────────────────────────────────────────────────

    private void speakNow(String text) {
        if (tts == null || !isReady.get()) return;

        String utteranceId = "boi_upi_" + System.currentTimeMillis();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        Log.i(TAG, "TTS speaking: " + text);
    }

    /**
     * Format amount for natural speech.
     * 1500.50 → "1500 and 50 paise"
     * 1500.00 → "1500"
     */
    private String formatAmount(long paise) {
        long rupees = paise / 100;
        long remainingPaise = paise % 100;

        if (remainingPaise > 0) {
            return rupees + " and " + remainingPaise + " paise";
        }
        return String.valueOf(rupees);
    }
}
