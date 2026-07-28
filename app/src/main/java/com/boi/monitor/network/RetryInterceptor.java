package com.boi.monitor.network;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that retries requests that fail with IOException or
 * SocketTimeoutException. Uses exponential backoff between retries.
 */
public class RetryInterceptor implements Interceptor {

    private static final String TAG = "RetryInterceptor";

    private final int maxRetries;
    private final long baseBackoffMillis;

    public RetryInterceptor(int maxRetries, long baseBackoffMillis) {
        this.maxRetries = maxRetries;
        this.baseBackoffMillis = baseBackoffMillis;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        IOException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                long backoff = baseBackoffMillis * (1L << (attempt - 1));
                Log.d(TAG, "[intercept] Retry " + attempt + "/" + maxRetries
                        + " for " + request.method() + " " + request.url().encodedPath()
                        + " waiting " + backoff + "ms");
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }

            try {
                Response response = chain.proceed(request);
                return response;
            } catch (IOException e) {
                lastException = e;
                Log.w(TAG, "[intercept] Attempt " + (attempt + 1) + "/" + (maxRetries + 1)
                        + " failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        Log.e(TAG, "[intercept] All " + (maxRetries + 1) + " attempts exhausted for "
                + request.method() + " " + request.url().encodedPath());
        throw lastException;
    }
}