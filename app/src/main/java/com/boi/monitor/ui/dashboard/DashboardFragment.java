package com.boi.monitor.ui.dashboard;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.boi.monitor.R;
import com.boi.monitor.databinding.FragmentDashboardBinding;
import com.boi.monitor.model.ChequeTransaction;
import com.boi.monitor.model.DashboardStats;
import com.boi.monitor.model.UpiTransaction;
import com.boi.monitor.network.ApiDataModule;
import com.boi.monitor.service.BOINotificationListenerService;
import com.boi.monitor.util.Constants;
import com.boi.monitor.util.FormatUtils;
import com.boi.monitor.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * DashboardFragment
 *
 * Overview screen: service status chip, stat cards, and recent transaction previews.
 * Uses the shared DashboardViewModel.
 */
public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private static final long REFRESH_DEBOUNCE_MS = 5000;

    private FragmentDashboardBinding binding;
    private DashboardViewModel       viewModel;
    private UpiTransactionAdapter    upiAdapter;
    private ChequeAdapter            chequeAdapter;
    private long                     lastRefreshTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupSwipeRefresh();
        setupRecyclerViews();
        setupViewModel();
        updatePermissionChip();
    }

    private void setupSwipeRefresh() {
        SwipeRefreshLayout swipeRefresh = binding.swipeRefresh;
        swipeRefresh.setOnRefreshListener(() -> {
            Log.d(TAG, "[setupSwipeRefresh] User pulled to refresh");
            ApiDataModule.getInstance().refreshAll();
            swipeRefresh.postDelayed(() -> {
                if (swipeRefresh.isRefreshing()) {
                    Log.w(TAG, "[setupSwipeRefresh] Force stopping refresh after timeout");
                    swipeRefresh.setRefreshing(false);
                }
            }, 5000);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "[onResume] Fragment resumed");
        updatePermissionChip();
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime > REFRESH_DEBOUNCE_MS) {
            lastRefreshTime = now;
            Log.d(TAG, "[onResume] Debounce passed, refreshing data");
            ApiDataModule.getInstance().refreshAll();
        } else {
            Log.d(TAG, "[onResume] Skipping refresh, debounced");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupRecyclerViews() {
        upiAdapter = new UpiTransactionAdapter(new ArrayList<>());
        binding.rvUpiTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUpiTransactions.setAdapter(upiAdapter);
        binding.rvUpiTransactions.setNestedScrollingEnabled(false);

        chequeAdapter = new ChequeAdapter(new ArrayList<>());
        binding.rvChequeTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChequeTransactions.setAdapter(chequeAdapter);
        binding.rvChequeTransactions.setNestedScrollingEnabled(false);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        viewModel.getDashboardStats().observe(getViewLifecycleOwner(), this::updateStatsUI);
        viewModel.getUpiTransactions().observe(getViewLifecycleOwner(), this::updateUpiList);
        viewModel.getChequeTransactions().observe(getViewLifecycleOwner(), this::updateChequeList);

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            stopRefresh();
            if (error != null && !error.isEmpty())
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateStatsUI(DashboardStats stats) {
        if (stats == null || binding == null) return;

        // UPI Card
        binding.tvTotalUpiAmount.setText(FormatUtils.formatCurrencyFromPaise(stats.getTotalUpiReceived()));
        binding.tvTotalUpiCount.setText(stats.getTotalUpiCount() + " transactions");

        // Cleared Card
        binding.tvTotalClearedAmount.setText(FormatUtils.formatCurrencyFromPaise(stats.getTotalClearedAmount()));
        binding.tvTotalClearedCount.setText(stats.getTotalClearedCount() + " cheques");

        // Returned Card
        binding.tvTotalReturnedAmount.setText(FormatUtils.formatCurrencyFromPaise(stats.getTotalReturnedAmount()));
        binding.tvTotalReturnedCount.setText(stats.getTotalReturnedCount() + " cheques");

        // Processing Card
        binding.tvTotalPresented.setText(String.valueOf(stats.getTotalPresentedCount()));
        binding.tvTotalProcessingAmount.setText(FormatUtils.formatCurrencyFromPaise(stats.getTotalProcessingAmount()));
    }

    private void stopRefresh() {
        SwipeRefreshLayout sr = binding.swipeRefresh;
        if (sr.isRefreshing()) sr.setRefreshing(false);
    }

    private void updateUpiList(List<UpiTransaction> list) {
        if (binding == null) return;
        stopRefresh();
        binding.progressUpi.setVisibility(View.GONE);
        if (list == null || list.isEmpty()) {
            binding.tvNoUpi.setVisibility(View.VISIBLE);
            binding.rvUpiTransactions.setVisibility(View.GONE);
        } else {
            binding.tvNoUpi.setVisibility(View.GONE);
            binding.rvUpiTransactions.setVisibility(View.VISIBLE);
            upiAdapter.updateData(list.subList(0, Math.min(list.size(), Constants.DASHBOARD_RECENT)));
        }
    }

    private void updateChequeList(List<ChequeTransaction> list) {
        if (binding == null) return;
        binding.progressCheque.setVisibility(View.GONE);
        if (list == null || list.isEmpty()) {
            binding.tvNoCheque.setVisibility(View.VISIBLE);
            binding.rvChequeTransactions.setVisibility(View.GONE);
        } else {
            binding.tvNoCheque.setVisibility(View.GONE);
            binding.rvChequeTransactions.setVisibility(View.VISIBLE);
            chequeAdapter.updateData(list.subList(0, Math.min(list.size(), Constants.DASHBOARD_RECENT)));
        }
    }

    private void updatePermissionChip() {
        if (binding == null) return;
        boolean enabled = isNotificationListenerEnabled();
        binding.chipServiceStatus.setText(enabled ? "● Service Active" : "○ Needs Permission");
        binding.chipServiceStatus.setChipBackgroundColorResource(
                enabled ? R.color.status_active : R.color.status_inactive);
        binding.chipServiceStatus.setOnClickListener(v -> {
            if (!enabled) requireContext().startActivity(
                    new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        });
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(
                requireContext().getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        String component = new ComponentName(requireContext(),
                BOINotificationListenerService.class).flattenToString();
        return flat.contains(component);
    }
}
