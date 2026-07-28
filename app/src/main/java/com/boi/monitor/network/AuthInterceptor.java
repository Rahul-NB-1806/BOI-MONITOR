package com.boi.monitor.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.boi.monitor.util.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that attaches the JWT Bearer token and API key to every request.
 * On 401/403 responses, automatically re-authenticates and retries the request once.
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private static final String PREFS_NAME = "boi_auth_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String HEADER_REAUTH = "X-Reauth-Attempted";

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();

        boolean alreadyReauthed = "true".equals(original.header(HEADER_REAUTH));

        Request.Builder builder = original.newBuilder()
                .header("X-API-Key", Constants.API_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        String token = getToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        Request request = builder.build();
        Response response = chain.proceed(request);

        if ((response.code() == 401 || response.code() == 403) && !alreadyReauthed) {
            Log.w(TAG, "[intercept] " + response.code() + " for " + request.method()
                    + " " + request.url().encodedPath() + " - attempting reauth");
            response.close();

            try {
                AuthManager authManager = AuthManager.getInstance();
                AuthResponse authResponse = authManager.anonymousAuthSync();
                Log.i(TAG, "[intercept] Reauth successful, userId=" + authResponse.getUserId());

                Request retryRequest = original.newBuilder()
                        .header("X-API-Key", Constants.API_KEY)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + authResponse.getToken())
                        .header(HEADER_REAUTH, "true")
                        .build();

                Response retryResponse = chain.proceed(retryRequest);
                if (retryResponse.code() == 401 || retryResponse.code() == 403) {
                    Log.e(TAG, "[intercept] Retry still failed with " + retryResponse.code());
                }
                return retryResponse;
            } catch (Exception e) {
                Log.e(TAG, "[intercept] Reauth failed: " + e.getMessage());
                throw new IOException("Re-authentication failed", e);
            }
        }

        if (response.code() == 401 || response.code() == 403) {
            Log.e(TAG, "[intercept] " + response.code() + " already reauthed, not retrying");
        }

        return response;
    }

    private String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_JWT_TOKEN, null);
    }
}
