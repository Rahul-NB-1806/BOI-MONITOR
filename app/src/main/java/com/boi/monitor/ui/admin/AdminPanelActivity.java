package com.boi.monitor.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.boi.monitor.R;
import com.boi.monitor.databinding.ActivityAdminPanelBinding;
import com.boi.monitor.network.ApiClient;
import com.boi.monitor.network.AuthManager;
import com.boi.monitor.network.BoiApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPanelActivity extends AppCompatActivity {

    private ActivityAdminPanelBinding binding;
    private AuthManager authManager;
    private BoiApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminPanelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Panel");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        authManager = AuthManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        loadStats();
        binding.btnRefresh.setOnClickListener(v -> loadStats());
    }

    private void loadStats() {
        String authToken = "Bearer " + authManager.getToken();

        apiService.getAdminStats().enqueue(new Callback<com.boi.monitor.model.DashboardStats>() {
            @Override
            public void onResponse(Call<com.boi.monitor.model.DashboardStats> call,
                                   Response<com.boi.monitor.model.DashboardStats> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.boi.monitor.model.DashboardStats stats = response.body();
                    binding.tvLogCount.setText("Total logs: " + stats.getTotalLogCount());
                    binding.tvProcessedCount.setText("Processed: " + stats.getProcessedLogCount());
                    binding.tvUnprocessedCount.setText("Unprocessed: " + stats.getUnprocessedLogCount());
                } else {
                    binding.tvLogCount.setText("Total logs: --");
                    binding.tvProcessedCount.setText("Processed: --");
                    binding.tvUnprocessedCount.setText("Unprocessed: --");
                }
            }

            @Override
            public void onFailure(Call<com.boi.monitor.model.DashboardStats> call, Throwable t) {
                Toast.makeText(AdminPanelActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvAppVersion.setText("BOI Monitor v1.0.0 — Production Prototype");
        binding.tvAdminUser.setText("Logged in as: " +
            (authManager.isLoggedIn() ? authManager.getUserId() : "Unknown"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Sign out of admin panel?")
                .setPositiveButton("Sign Out", (d, w) -> {
                    authManager.logout();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
