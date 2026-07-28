package com.boi.monitor.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manages JWT authentication state: login, register, anonymous auth, token storage.
 * Replaces FirebaseAuthManager for the REST API migration.
 */
public class AuthManager {

    private static final String TAG = "AuthManager";
    private static final String PREFS_NAME = "boi_auth_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_IS_ANONYMOUS = "is_anonymous";

    public interface AuthCallback {
        void onSuccess(String userId, String token);
        void onFailure(String errorMessage);
    }

    private static volatile AuthManager instance;
    private final SharedPreferences prefs;

    private AuthManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private BoiApiService getApi() {
        return ApiClient.getInstance().getApiService();
    }

    public static AuthManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthManager.class) {
                if (instance == null) {
                    instance = new AuthManager(context);
                }
            }
        }
        return instance;
    }

    public static AuthManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "AuthManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    // ── Auth Operations ────────────────────────────────────────────────────────

    /**
     * Register with email and password.
     */
    public void register(String email, String password, @NonNull AuthCallback callback) {
        RegisterRequest request = new RegisterRequest(email, password);
        getApi().register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                   @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse body = response.body();
                    saveAuthState(body.getToken(), body.getUserId(), false);
                    Log.i(TAG, "Register success: userId=" + body.getUserId());
                    callback.onSuccess(body.getUserId(), body.getToken());
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Register failed: " + error);
                    callback.onFailure(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Register network error", t);
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Login with email and password.
     */
    public void login(String email, String password, @NonNull AuthCallback callback) {
        LoginRequest request = new LoginRequest(email, password);
        getApi().login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                   @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse body = response.body();
                    saveAuthState(body.getToken(), body.getUserId(), false);
                    Log.i(TAG, "Login success: userId=" + body.getUserId());
                    callback.onSuccess(body.getUserId(), body.getToken());
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Login failed: " + error);
                    callback.onFailure(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Login network error", t);
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Authenticate anonymously using an API key. Used as default app auth state.
     */
    public void anonymousAuth(@NonNull AuthCallback callback) {
        String cachedUserId = getUserId();
        AnonymousAuthRequest body = new AnonymousAuthRequest(cachedUserId);
        getApi().anonymousAuth(com.boi.monitor.util.Constants.API_KEY, body)
                .enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                   @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse body = response.body();
                    saveAuthState(body.getToken(), body.getUserId(), true);
                    Log.i(TAG, "Anonymous auth success: userId=" + body.getUserId());
                    callback.onSuccess(body.getUserId(), body.getToken());
                } else {
                    String error = parseError(response);
                    Log.e(TAG, "Anonymous auth failed: " + error);
                    callback.onFailure(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Anonymous auth network error", t);
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Synchronous anonymous auth for use in OkHttp interceptors.
     * Returns the AuthResponse or throws IOException on failure.
     */
    public AuthResponse anonymousAuthSync() throws IOException {
        Log.d(TAG, "[anonymousAuthSync] Starting synchronous anonymous auth");
        String cachedUserId = getUserId();
        AnonymousAuthRequest body = new AnonymousAuthRequest(cachedUserId);
        Response<AuthResponse> response = getApi()
                .anonymousAuth(com.boi.monitor.util.Constants.API_KEY, body)
                .execute();
        if (response.isSuccessful() && response.body() != null) {
            AuthResponse authResponse = response.body();
            saveAuthState(authResponse.getToken(), authResponse.getUserId(), true);
            Log.i(TAG, "[anonymousAuthSync] Auth success: userId=" + authResponse.getUserId());
            return authResponse;
        } else {
            String error = parseError(response);
            Log.e(TAG, "[anonymousAuthSync] Auth failed: " + error);
            throw new IOException("Anonymous auth failed: " + error);
        }
    }

    /**
     * Clear local auth state. Does NOT call a server-side revoke endpoint.
     */
    public void logout() {
        prefs.edit()
                .remove(KEY_JWT_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_IS_ANONYMOUS)
                .apply();
        Log.i(TAG, "Logged out, auth state cleared");
    }

    // ── Auth State Queries ─────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return getToken() != null && getUserId() != null;
    }

    public String getToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public boolean isAnonymous() {
        return prefs.getBoolean(KEY_IS_ANONYMOUS, false);
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private void saveAuthState(String token, String userId, boolean anonymous) {
        prefs.edit()
                .putString(KEY_JWT_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .putBoolean(KEY_IS_ANONYMOUS, anonymous)
                .apply();
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return response.errorBody().string();
            }
        } catch (Exception ignored) {}
        return "HTTP " + response.code() + " " + response.message();
    }
}
