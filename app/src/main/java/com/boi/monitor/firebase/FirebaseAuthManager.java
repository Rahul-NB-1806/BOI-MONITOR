package com.boi.monitor.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * FirebaseAuthManager
 *
 * Wraps Firebase Authentication operations with clean callbacks.
 * Keeps auth logic out of Activities.
 */
public class FirebaseAuthManager {

    private static final String TAG = "FirebaseAuthManager";

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }

    private static volatile FirebaseAuthManager instance;
    private final FirebaseAuth auth;

    private FirebaseAuthManager() {
        auth = FirebaseAuth.getInstance();
    }

    public static FirebaseAuthManager getInstance() {
        if (instance == null) {
            synchronized (FirebaseAuthManager.class) {
                if (instance == null) instance = new FirebaseAuthManager();
            }
        }
        return instance;
    }

    // ── Auth Operations ────────────────────────────────────────────────────────

    /**
     * Sign in anonymously. Used as the default auth state for notification monitoring.
     */
    public void signInAnonymously(@NonNull AuthCallback callback) {
        auth.signInAnonymously()
            .addOnSuccessListener(result -> {
                Log.i(TAG, "Anonymous sign-in success: " + result.getUser().getUid());
                callback.onSuccess(result.getUser());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Anonymous sign-in failed", e);
                callback.onFailure(e.getMessage());
            });
    }

    /**
     * Sign in with email and password.
     */
    public void signIn(@NonNull String email, @NonNull String password,
                       @NonNull AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                Log.i(TAG, "Sign-in success: " + result.getUser().getEmail());
                callback.onSuccess(result.getUser());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Sign-in failed", e);
                callback.onFailure(e.getMessage());
            });
    }

    /**
     * Sign out the current user.
     */
    public void signOut() {
        String email = getCurrentUserEmail();
        auth.signOut();
        Log.i(TAG, "Signed out: " + email);
    }

    /**
     * Returns the currently authenticated user, or null if not signed in.
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null ? u.getEmail() : "none";
    }
}
