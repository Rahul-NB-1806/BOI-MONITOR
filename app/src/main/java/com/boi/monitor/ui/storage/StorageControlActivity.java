package com.boi.monitor.ui.storage;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.boi.monitor.R;
import com.boi.monitor.network.ApiDataModule;
import com.boi.monitor.model.StorageStats;

import java.util.Calendar;

public class StorageControlActivity extends AppCompatActivity {

    private ApiDataModule dataModule;

    private TextView bannerWarning;
    private TextView tvUpiInfo, tvChequeInfo, tvLogInfo, tvTotalInfo;
    private ProgressBar progressUpi, progressCheque, progressLog, progressTotal;

    private StorageStats currentStats;
    private String selectedDate;

    private static final double PER_CATEGORY_CAP_MB = 5.0;
    private static final double TOTAL_CAP_MB = 100.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_control);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Storage Control");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dataModule = ApiDataModule.getInstance();

        bannerWarning = findViewById(R.id.banner_warning);
        tvUpiInfo = findViewById(R.id.tv_upi_info);
        tvChequeInfo = findViewById(R.id.tv_cheque_info);
        tvLogInfo = findViewById(R.id.tv_log_info);
        tvTotalInfo = findViewById(R.id.tv_total_info);
        progressUpi = findViewById(R.id.progress_upi);
        progressCheque = findViewById(R.id.progress_cheque);
        progressLog = findViewById(R.id.progress_log);
        progressTotal = findViewById(R.id.progress_total);

        findViewById(R.id.btn_delete_upi).setOnClickListener(v -> confirmDelete("UPI", "upi"));
        findViewById(R.id.btn_delete_cheque).setOnClickListener(v -> confirmDelete("Cheque", "cheque"));
        findViewById(R.id.btn_delete_log).setOnClickListener(v -> confirmDelete("Log", "log"));

        findViewById(R.id.btn_pick_date).setOnClickListener(v -> showDatePicker());

        findViewById(R.id.btn_delete_selected).setOnClickListener(v -> confirmDateDelete());

        loadStats();
    }

    private void loadStats() {
        dataModule.getStorageStats(new ApiDataModule.ApiCallback<StorageStats>() {
            @Override
            public void onSuccess(StorageStats stats) {
                currentStats = stats;
                updateUI(stats);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(StorageControlActivity.this,
                        "Failed to load stats: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(StorageStats stats) {
        if (stats == null) return;

        updateCategory(tvUpiInfo, progressUpi, R.drawable.progress_bar_green, stats.getUpi());
        updateCategory(tvChequeInfo, progressCheque, R.drawable.progress_bar_green, stats.getCheques());
        updateCategory(tvLogInfo, progressLog, R.drawable.progress_bar_green, stats.getLogs());

        StorageStats.CategoryStats total = stats.getTotal();
        tvTotalInfo.setText(String.format("%.1f MB / %.0f MB", total.getEstimatedMB(), TOTAL_CAP_MB));
        int totalPct = (int) Math.min(100, (total.getEstimatedMB() / TOTAL_CAP_MB) * 100);
        progressTotal.setProgress(totalPct);
        updateProgressColor(progressTotal, totalPct, R.drawable.progress_bar_green);

        updateBanner(stats);
    }

    private void updateCategory(TextView infoView, ProgressBar bar, int defaultDrawable,
                                 StorageStats.CategoryStats cat) {
        String text = String.format("Count: %d  |  %.1f MB / %.0f MB",
                cat.getCount(), cat.getEstimatedMB(), PER_CATEGORY_CAP_MB);
        infoView.setText(text);

        int pct = (int) Math.min(100, (cat.getEstimatedMB() / PER_CATEGORY_CAP_MB) * 100);
        bar.setProgress(pct);
        updateProgressColor(bar, pct, defaultDrawable);
    }

    private void updateProgressColor(ProgressBar bar, int pct, int defaultDrawable) {
        if (pct >= 90) {
            bar.setProgressDrawable(getDrawable(R.drawable.progress_bar_red));
        } else if (pct >= 80) {
            bar.setProgressDrawable(getDrawable(R.drawable.progress_bar_amber));
        } else {
            bar.setProgressDrawable(getDrawable(defaultDrawable));
        }
    }

    private void updateBanner(StorageStats stats) {
        StorageStats.CategoryStats upi = stats.getUpi();
        StorageStats.CategoryStats cheques = stats.getCheques();
        StorageStats.CategoryStats logs = stats.getLogs();
        StorageStats.CategoryStats total = stats.getTotal();

        double maxPct = Math.max(
            Math.max(upi.getEstimatedMB() / PER_CATEGORY_CAP_MB,
                     cheques.getEstimatedMB() / PER_CATEGORY_CAP_MB),
            Math.max(logs.getEstimatedMB() / PER_CATEGORY_CAP_MB,
                     total.getEstimatedMB() / TOTAL_CAP_MB)
        ) * 100;

        if (maxPct >= 90) {
            bannerWarning.setVisibility(View.VISIBLE);
            bannerWarning.setText("\u26A0 Storage almost full! Delete old records soon");
            bannerWarning.setBackgroundColor(getColor(R.color.banner_red_bg));
            bannerWarning.setTextColor(getColor(R.color.warning_red));
        } else if (maxPct >= 80) {
            bannerWarning.setVisibility(View.VISIBLE);
            bannerWarning.setText("Storage nearly full \u2014 consider deleting old data");
            bannerWarning.setBackgroundColor(getColor(R.color.banner_amber_bg));
            bannerWarning.setTextColor(getColor(R.color.warning_amber));
        } else {
            bannerWarning.setVisibility(View.GONE);
        }
    }

    private void confirmDelete(String label, String type) {
        int count = 0;
        if ("upi".equals(type) && currentStats != null) count = currentStats.getUpi().getCount();
        if ("cheque".equals(type) && currentStats != null) count = currentStats.getCheques().getCount();
        if ("log".equals(type) && currentStats != null) count = currentStats.getLogs().getCount();

        new AlertDialog.Builder(this)
                .setTitle("Delete All " + label)
                .setMessage("This will permanently delete " + count + " " + label.toLowerCase()
                        + " record(s). This action cannot be undone. Continue?")
                .setPositiveButton("Delete", (d, w) -> executeDelete(type))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDelete(String type) {
        ApiDataModule.ApiCallback<Integer> callback = new ApiDataModule.ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                Toast.makeText(StorageControlActivity.this,
                        "Deleted successfully", Toast.LENGTH_SHORT).show();
                loadStats();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(StorageControlActivity.this,
                        "Delete failed: " + error, Toast.LENGTH_SHORT).show();
            }
        };

        switch (type) {
            case "upi": dataModule.deleteAllUpi(callback); break;
            case "cheque": dataModule.deleteAllCheques(callback); break;
            case "log": dataModule.deleteAllLogs(callback); break;
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            Toast.makeText(this, "Selected: " + selectedDate, Toast.LENGTH_SHORT).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmDateDelete() {
        String daysStr = ((android.widget.EditText) findViewById(R.id.et_days)).getText().toString().trim();

        if (daysStr.isEmpty() && selectedDate == null) {
            Toast.makeText(this, "Enter days or pick a date", Toast.LENGTH_SHORT).show();
            return;
        }

        String message;
        if (selectedDate != null) {
            message = "Delete all records older than " + selectedDate + "?";
        } else {
            message = "Delete all records older than " + daysStr + " days?";
        }

        new AlertDialog.Builder(this)
                .setTitle("Date-wise Delete")
                .setMessage(message + " This affects UPI, cheque, and notification logs.")
                .setPositiveButton("Delete", (d, w) -> executeDateDelete(daysStr))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDateDelete(String daysStr) {
        ApiDataModule.ApiCallback<Integer> refreshCallback = new ApiDataModule.ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                loadStats();
            }

            @Override
            public void onFailure(String error) { }
        };

        int days = daysStr.isEmpty() ? 0 : Integer.parseInt(daysStr);

        if (days > 0) {
            dataModule.deleteUpiOlderThan(days, refreshCallback);
            dataModule.deleteChequesOlderThan(days, refreshCallback);
            dataModule.deleteLogsOlderThan(days, refreshCallback);
        }

        if (selectedDate != null) {
            dataModule.deleteUpiOlderThanDate(selectedDate, refreshCallback);
            dataModule.deleteChequesOlderThanDate(selectedDate, refreshCallback);
            dataModule.deleteLogsOlderThanDate(selectedDate, refreshCallback);
        }

        Toast.makeText(this, "Deletion in progress...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
