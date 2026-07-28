package com.boi.monitor.network;

import com.boi.monitor.model.ChequeTransaction;
import com.boi.monitor.model.DashboardStats;
import com.boi.monitor.model.NotificationLog;
import com.boi.monitor.model.StorageStats;
import com.boi.monitor.model.UpiTransaction;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit service interface for the BOI Monitor REST API.
 */
public interface BoiApiService {

    // ── Auth ───────────────────────────────────────────────────────────────────

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/anonymous")
    Call<AuthResponse> anonymousAuth(@Header("X-API-Key") String apiKey, @Body AnonymousAuthRequest request);

    // ── UPI Transactions ──────────────────────────────────────────────────────

    @GET("api/upi")
    Call<List<UpiTransaction>> getUpiTransactions(@Query("limit") int limit);

    @POST("api/upi")
    Call<Void> saveUpiTransaction(@Body UpiTransaction tx);

    // ── Cheque Transactions ───────────────────────────────────────────────────

    @GET("api/cheques")
    Call<List<ChequeTransaction>> getChequeTransactions(@Query("limit") int limit);

    @POST("api/cheques")
    Call<Void> saveChequeTransaction(@Body ChequeTransaction tx);

    // ── Notification Logs ─────────────────────────────────────────────────────

    @GET("api/logs")
    Call<List<NotificationLog>> getLogs(@Query("limit") int limit);

    @POST("api/logs")
    Call<Void> saveLog(@Body NotificationLog log);

    // ── Storage / Delete ──────────────────────────────────────────────────────

    @GET("api/storage/stats")
    Call<StorageStats> getStorageStats();

    @DELETE("api/upi")
    Call<Void> deleteAllUpi();

    @DELETE("api/upi/older-than")
    Call<Void> deleteUpiOlderThan(@Body DeleteRequest request);

    @DELETE("api/cheques")
    Call<Void> deleteAllCheques();

    @DELETE("api/cheques/older-than")
    Call<Void> deleteChequesOlderThan(@Body DeleteRequest request);

    @DELETE("api/logs")
    Call<Void> deleteAllLogs();

    @DELETE("api/logs/older-than")
    Call<Void> deleteLogsOlderThan(@Body DeleteRequest request);

    // ── User Data ─────────────────────────────────────────────────────────────

    @DELETE("api/user/data")
    Call<Void> deleteAllData();

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GET("api/admin/stats")
    Call<DashboardStats> getAdminStats();
}
