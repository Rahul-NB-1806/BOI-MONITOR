package com.boi.monitor.ui.cheque;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.boi.monitor.databinding.FragmentChequeManagementBinding;
import com.boi.monitor.model.ChequeTransaction;
import com.boi.monitor.ui.dashboard.ChequeAdapter;
import com.boi.monitor.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ChequeManagementFragment
 *
 * Full-screen cheque list with filter by status (All / Cleared / Returned / Presented).
 * Backed by the shared DashboardViewModel (same Firestore listener).
 */
public class ChequeManagementFragment extends Fragment {

    private FragmentChequeManagementBinding binding;
    private DashboardViewModel viewModel;
    private ChequeAdapter adapter;

    private String currentFilter = "ALL";

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChequeManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViewModel();
        setupRecyclerView();
        setupFilterSpinner();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new ChequeAdapter(new ArrayList<>());
        binding.rvCheques.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCheques.setAdapter(adapter);
    }

    private void setupFilterSpinner() {
        String[] filters = {"All", "Processing", "Cleared", "Returned", "Presented"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                filters
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                switch (pos) {
                    case 0: currentFilter = "ALL";       break;
                    case 1:currentFilter = "PROCESSING"; break;
                    case 2: currentFilter = "CLEARED";   break;
                    case 3: currentFilter = "RETURNED";  break;
                    case 4: currentFilter = "PRESENTED"; break;
                }
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        viewModel.getChequeTransactions().observe(getViewLifecycleOwner(), list -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(View.GONE);
            applyFilter();
        });
        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
        });
    }

    // ── Filter Logic ───────────────────────────────────────────────────────────

    private void applyFilter() {
        if (binding == null || adapter == null || viewModel == null) return;
        List<ChequeTransaction> all = viewModel.getChequeTransactions().getValue();
        if (all == null) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        List<ChequeTransaction> filtered;
        if ("ALL".equals(currentFilter)) {

            filtered = all;

        } else if ("PROCESSING".equals(currentFilter)) {

            filtered = all.stream()
                    .filter(tx -> !ChequeTransaction.STATUS_CLEARED.equals(tx.getStatus())
                            && !ChequeTransaction.STATUS_RETURNED.equals(tx.getStatus()))
                    .collect(Collectors.toList());

        } else {

            final String f = currentFilter;

            filtered = all.stream()
                    .filter(tx -> f.equals(tx.getStatus()))
                    .collect(Collectors.toList());
        }

        adapter.updateData(filtered);

        // Update count label
        binding.tvResultCount.setText(filtered.size() + " cheques");

        // Show/hide empty state
        if (filtered.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvCheques.setVisibility(View.GONE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvCheques.setVisibility(View.VISIBLE);
        }
    }
}
