package com.boi.monitor.network;

import android.content.Context;
import android.util.Log;

import com.boi.monitor.util.Constants;
import com.boi.monitor.util.PrefsManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client configured with OkHttp, auth interceptor,
 * logging (debug builds), and Gson serialization.
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final long CONNECT_TIMEOUT_SECONDS = 30;
    private static final long READ_TIMEOUT_SECONDS = 30;
    private static final long WRITE_TIMEOUT_SECONDS = 30;

    private static volatile ApiClient instance;
    private BoiApiService apiService;
    private OkHttpClient okHttpClient;
    private final Context appContext;
    private String currentBaseUrl;

    private ApiClient(Context context) {
        this.appContext = context.getApplicationContext();
        buildClient();
    }

    private void buildClient() {
        String baseUrl = PrefsManager.getInstance(appContext).getServerUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        currentBaseUrl = baseUrl;

        AuthInterceptor authInterceptor = new AuthInterceptor(appContext);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor);

        okHttpClient = okBuilder.build();

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(BoiApiService.class);

        Log.i(TAG, "ApiClient initialized with baseUrl=" + currentBaseUrl);
    }

    public synchronized void reconfigure() {
        String newUrl = PrefsManager.getInstance(appContext).getServerUrl();
        if (!newUrl.endsWith("/")) newUrl += "/";
        if (newUrl.equals(currentBaseUrl)) return;
        Log.i(TAG, "Reconfiguring ApiClient: " + currentBaseUrl + " -> " + newUrl);
        buildClient();
    }

    public static ApiClient getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient(context);
                }
            }
        }
        return instance;
    }

    /**
     * Use only when context is already available from Application.
     */
    public static ApiClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "ApiClient not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    public BoiApiService getApiService() {
        return apiService;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public Context getContext() {
        return appContext;
    }
}
